"use strict";

const crypto = require("crypto");
const nodemailer = require("nodemailer");

const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const {
  getFirestore,
  Timestamp,
} = require("firebase-admin/firestore");

const {
  defineSecret,
  defineString,
} = require("firebase-functions/params");

const {
  onCall,
  onRequest,
  HttpsError,
} = require("firebase-functions/v2/https");

initializeApp();

const db = getFirestore();

const REGION = "europe-west1";
const PROJECT_ID = "shooterranking-2cfbb";
const REQUEST_COLLECTION = "accountDeletionRequests";
const REQUEST_TTL_MS = 30 * 60 * 1000;
const RESEND_COOLDOWN_MS = 60 * 1000;
const DELETE_BATCH_SIZE = 400;

const SMTP_HOST = defineString("SMTP_HOST");
const SMTP_PORT = defineString("SMTP_PORT", { default: "587" });
const SMTP_SECURE = defineString("SMTP_SECURE", { default: "false" });
const MAIL_FROM = defineString("MAIL_FROM");

const SMTP_USER = defineSecret("SMTP_USER");
const SMTP_PASSWORD = defineSecret("SMTP_PASSWORD");

function normalizeLanguage(language) {
  const supported = new Set(["es", "ca", "en", "fr"]);
  const normalized = String(language || "").toLowerCase();
  return supported.has(normalized) ? normalized : "es";
}

function sha256(value) {
  return crypto
    .createHash("sha256")
    .update(value)
    .digest("hex");
}

function tokensMatch(storedHash, suppliedToken) {
  if (
    typeof storedHash !== "string" ||
    typeof suppliedToken !== "string"
  ) {
    return false;
  }

  const candidateHash = sha256(suppliedToken);

  if (storedHash.length !== candidateHash.length) {
    return false;
  }

  try {
    return crypto.timingSafeEqual(
      Buffer.from(storedHash, "hex"),
      Buffer.from(candidateHash, "hex")
    );
  } catch (_) {
    return false;
  }
}

function translations(language) {
  const all = {
    es: {
      subject: "Confirma la eliminación de tu cuenta de Shooter Ranking",
      emailTitle: "Eliminar cuenta de Shooter Ranking",
      emailText:
        "Has solicitado eliminar tu cuenta de Shooter Ranking. Pulsa el botón para continuar. El enlace caduca en 30 minutos.",
      emailButton: "Continuar con la eliminación",
      emailIgnore:
        "Si no has solicitado eliminar tu cuenta, puedes ignorar este correo.",
      pageTitle: "Eliminar cuenta",
      pageText:
        "Estás a punto de eliminar permanentemente tu cuenta de Shooter Ranking y todos los datos asociados.",
      pageWarning: "Esta acción no se puede deshacer.",
      pageButton: "Eliminar mi cuenta definitivamente",
      invalidTitle: "Enlace no válido",
      invalidText:
        "El enlace ha caducado, ya se ha utilizado o no es válido.",
      successTitle: "Cuenta eliminada",
      successText:
        "Tu cuenta de Shooter Ranking y sus datos asociados se han eliminado correctamente.",
      errorTitle: "No se ha podido eliminar la cuenta",
      errorText:
        "Se ha producido un error durante la eliminación. Inténtalo de nuevo.",
    },
    ca: {
      subject: "Confirma l’eliminació del teu compte de Shooter Ranking",
      emailTitle: "Eliminar el compte de Shooter Ranking",
      emailText:
        "Has sol·licitat eliminar el teu compte de Shooter Ranking. Prem el botó per continuar. L’enllaç caduca en 30 minuts.",
      emailButton: "Continuar amb l’eliminació",
      emailIgnore:
        "Si no has sol·licitat eliminar el compte, pots ignorar aquest correu.",
      pageTitle: "Eliminar el compte",
      pageText:
        "Estàs a punt d’eliminar permanentment el teu compte de Shooter Ranking i totes les dades associades.",
      pageWarning: "Aquesta acció no es pot desfer.",
      pageButton: "Eliminar definitivament el meu compte",
      invalidTitle: "Enllaç no vàlid",
      invalidText:
        "L’enllaç ha caducat, ja s’ha utilitzat o no és vàlid.",
      successTitle: "Compte eliminat",
      successText:
        "El teu compte de Shooter Ranking i les dades associades s’han eliminat correctament.",
      errorTitle: "No s’ha pogut eliminar el compte",
      errorText:
        "S’ha produït un error durant l’eliminació. Torna-ho a provar.",
    },
    en: {
      subject: "Confirm deletion of your Shooter Ranking account",
      emailTitle: "Delete your Shooter Ranking account",
      emailText:
        "You requested deletion of your Shooter Ranking account. Press the button below to continue. The link expires in 30 minutes.",
      emailButton: "Continue account deletion",
      emailIgnore:
        "If you did not request this, you can safely ignore this email.",
      pageTitle: "Delete account",
      pageText:
        "You are about to permanently delete your Shooter Ranking account and all associated data.",
      pageWarning: "This action cannot be undone.",
      pageButton: "Permanently delete my account",
      invalidTitle: "Invalid link",
      invalidText:
        "This link has expired, has already been used, or is invalid.",
      successTitle: "Account deleted",
      successText:
        "Your Shooter Ranking account and associated data have been successfully deleted.",
      errorTitle: "Unable to delete account",
      errorText:
        "An error occurred while deleting the account. Please try again.",
    },
    fr: {
      subject: "Confirmez la suppression de votre compte Shooter Ranking",
      emailTitle: "Supprimer votre compte Shooter Ranking",
      emailText:
        "Vous avez demandé la suppression de votre compte Shooter Ranking. Appuyez sur le bouton pour continuer. Le lien expire dans 30 minutes.",
      emailButton: "Continuer la suppression",
      emailIgnore:
        "Si vous n’avez pas demandé cette suppression, vous pouvez ignorer cet e-mail.",
      pageTitle: "Supprimer le compte",
      pageText:
        "Vous êtes sur le point de supprimer définitivement votre compte Shooter Ranking et toutes les données associées.",
      pageWarning: "Cette action est irréversible.",
      pageButton: "Supprimer définitivement mon compte",
      invalidTitle: "Lien non valide",
      invalidText:
        "Ce lien a expiré, a déjà été utilisé ou n’est pas valide.",
      successTitle: "Compte supprimé",
      successText:
        "Votre compte Shooter Ranking et les données associées ont été supprimés avec succès.",
      errorTitle: "Impossible de supprimer le compte",
      errorText:
        "Une erreur s’est produite pendant la suppression. Veuillez réessayer.",
    },
  };

  return all[normalizeLanguage(language)];
}

function buildPage(language, title, bodyHtml) {
  return `<!doctype html>
<html lang="${normalizeLanguage(language)}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="robots" content="noindex,nofollow">
  <title>${title}</title>
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: #0F1223;
      color: #EDEFF6;
      font-family: Arial, Helvetica, sans-serif;
    }
    .card {
      width: 100%;
      max-width: 560px;
      padding: 30px;
      border-radius: 22px;
      background: #141834;
      box-shadow: 0 20px 60px rgba(0, 0, 0, .35);
    }
    .brand {
      margin-bottom: 22px;
      color: #3BB54A;
      font-weight: 700;
    }
    h1 { margin: 0 0 16px; }
    p { color: #C9CDE0; line-height: 1.55; }
    .warning { color: #FF8080; font-weight: 700; }
    button {
      width: 100%;
      border: 0;
      border-radius: 16px;
      padding: 16px;
      margin-top: 20px;
      background: #FF4D4D;
      color: #fff;
      font-size: 16px;
      font-weight: 700;
      cursor: pointer;
    }
  </style>
</head>
<body>
  <main class="card">
    <div class="brand">Shooter Ranking</div>
    ${bodyHtml}
  </main>
</body>
</html>`;
}

function confirmationUrl(uid, token) {
  return (
    `https://${REGION}-${PROJECT_ID}.cloudfunctions.net/confirmAccountDeletion` +
    `?uid=${encodeURIComponent(uid)}` +
    `&token=${encodeURIComponent(token)}`
  );
}

function createTransporter() {
  return nodemailer.createTransport({
    host: SMTP_HOST.value(),
    port: Number(SMTP_PORT.value()),
    secure: SMTP_SECURE.value().toLowerCase() === "true",
    auth: {
      user: SMTP_USER.value(),
      pass: SMTP_PASSWORD.value(),
    },
  });
}

async function getValidRequest(uid, token) {
  if (!uid || !token) {
    return null;
  }

  const ref = db
    .collection(REQUEST_COLLECTION)
    .doc(uid);

  const snapshot = await ref.get();

  if (!snapshot.exists) {
    return null;
  }

  const data = snapshot.data();

  if (!data || !tokensMatch(data.tokenHash, token)) {
    return null;
  }

  const expiresAt = data.expiresAt;

  if (
    !expiresAt ||
    typeof expiresAt.toMillis !== "function" ||
    expiresAt.toMillis() < Date.now()
  ) {
    return null;
  }

  return {
    ref,
    data,
  };
}

async function deleteDocumentsByField(
  collectionName,
  fieldName,
  value
) {
  while (true) {
    const snapshot = await db
      .collection(collectionName)
      .where(fieldName, "==", value)
      .limit(DELETE_BATCH_SIZE)
      .get();

    if (snapshot.empty) {
      return;
    }

    const batch = db.batch();

    snapshot.docs.forEach((document) => {
      batch.delete(document.ref);
    });

    await batch.commit();
  }
}

function bodyValue(request, key) {
  if (
    request.body &&
    typeof request.body === "object" &&
    !Buffer.isBuffer(request.body)
  ) {
    return request.body[key];
  }

  const raw = request.rawBody
    ? request.rawBody.toString("utf8")
    : "";

  return new URLSearchParams(raw).get(key);
}

exports.requestAccountDeletion = onCall(
  {
    region: REGION,
    secrets: [SMTP_USER, SMTP_PASSWORD],
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "Authentication required."
      );
    }

    const uid = request.auth.uid;
    const authUser = await getAuth().getUser(uid);

    if (!authUser.email) {
      throw new HttpsError(
        "failed-precondition",
        "The account has no email address."
      );
    }

    if (!authUser.emailVerified) {
      throw new HttpsError(
        "failed-precondition",
        "The email address is not verified."
      );
    }

    const language = normalizeLanguage(
      request.data?.language
    );

    const requestRef = db
      .collection(REQUEST_COLLECTION)
      .doc(uid);

    const previous = await requestRef.get();

    if (previous.exists) {
      const requestedAt = previous.data()?.requestedAt;

      if (
        requestedAt &&
        typeof requestedAt.toMillis === "function" &&
        Date.now() - requestedAt.toMillis() < RESEND_COOLDOWN_MS
      ) {
        throw new HttpsError(
          "resource-exhausted",
          "Please wait before requesting another deletion email."
        );
      }
    }

    const token = crypto
      .randomBytes(32)
      .toString("hex");

    await requestRef.set({
      tokenHash: sha256(token),
      email: authUser.email,
      language,
      requestedAt: Timestamp.now(),
      expiresAt: Timestamp.fromMillis(
        Date.now() + REQUEST_TTL_MS
      ),
    });

    const t = translations(language);
    const url = confirmationUrl(uid, token);

    try {
      const transporter = createTransporter();

      await transporter.sendMail({
        from: MAIL_FROM.value(),
        to: authUser.email,
        subject: t.subject,
        text: `${t.emailText}\n\n${url}\n\n${t.emailIgnore}`,
        html: `
          <div style="font-family:Arial,sans-serif;background:#0F1223;padding:32px;color:#EDEFF6">
            <div style="max-width:560px;margin:auto;background:#141834;border-radius:20px;padding:28px">
              <div style="color:#3BB54A;font-weight:bold;margin-bottom:20px">Shooter Ranking</div>
              <h2>${t.emailTitle}</h2>
              <p style="color:#C9CDE0;line-height:1.6">${t.emailText}</p>
              <a href="${url}" style="display:block;text-align:center;margin-top:24px;padding:15px;border-radius:14px;background:#FF4D4D;color:#fff;text-decoration:none;font-weight:bold">
                ${t.emailButton}
              </a>
              <p style="margin-top:24px;color:#C9CDE0;font-size:13px;line-height:1.5">${t.emailIgnore}</p>
            </div>
          </div>
        `,
      });
    } catch (error) {
      console.error("Unable to send deletion email:", error);
      await requestRef.delete();

      throw new HttpsError(
        "internal",
        "Unable to send deletion email."
      );
    }

    return {
      success: true,
    };
  }
);

exports.confirmAccountDeletion = onRequest(
  {
    region: REGION,
  },
  async (request, response) => {
    response.set("Cache-Control", "no-store, max-age=0");

    if (request.method === "GET") {
      const uid = String(request.query.uid || "");
      const token = String(request.query.token || "");

      const validRequest = await getValidRequest(
        uid,
        token
      );

      if (!validRequest) {
        const t = translations("es");

        response
          .status(400)
          .type("html")
          .send(
            buildPage(
              "es",
              t.invalidTitle,
              `<h1>${t.invalidTitle}</h1><p>${t.invalidText}</p>`
            )
          );

        return;
      }

      const language = normalizeLanguage(
        validRequest.data.language
      );
      const t = translations(language);

      response
        .status(200)
        .type("html")
        .send(
          buildPage(
            language,
            t.pageTitle,
            `
              <h1>${t.pageTitle}</h1>
              <p>${t.pageText}</p>
              <p class="warning">${t.pageWarning}</p>
              <form method="POST" action="">
                <input type="hidden" name="uid" value="${uid}">
                <input type="hidden" name="token" value="${token}">
                <button type="submit">${t.pageButton}</button>
              </form>
            `
          )
        );

      return;
    }

    if (request.method !== "POST") {
      response
        .status(405)
        .send("Method Not Allowed");
      return;
    }

    const uid = String(bodyValue(request, "uid") || "");
    const token = String(bodyValue(request, "token") || "");

    const validRequest = await getValidRequest(
      uid,
      token
    );

    if (!validRequest) {
      const t = translations("es");

      response
        .status(400)
        .type("html")
        .send(
          buildPage(
            "es",
            t.invalidTitle,
            `<h1>${t.invalidTitle}</h1><p>${t.invalidText}</p>`
          )
        );

      return;
    }

    const language = normalizeLanguage(
      validRequest.data.language
    );
    const t = translations(language);

    try {
      await deleteDocumentsByField(
        "sessions",
        "userId",
        uid
      );

      await deleteDocumentsByField(
        "jugadors",
        "userId",
        uid
      );

      await deleteDocumentsByField(
        "equips",
        "userId",
        uid
      );

      await deleteDocumentsByField(
        "temporades",
        "userId",
        uid
      );

      await deleteDocumentsByField(
        "usernames",
        "uid",
        uid
      );

      await db
        .collection("users")
        .doc(uid)
        .delete();

      try {
        await getAuth().deleteUser(uid);
      } catch (error) {
        if (error?.code !== "auth/user-not-found") {
          throw error;
        }
      }

      await validRequest.ref.delete();

      response
        .status(200)
        .type("html")
        .send(
          buildPage(
            language,
            t.successTitle,
            `<h1>${t.successTitle}</h1><p>${t.successText}</p>`
          )
        );
    } catch (error) {
      console.error("Account deletion failed:", error);

      response
        .status(500)
        .type("html")
        .send(
          buildPage(
            language,
            t.errorTitle,
            `<h1>${t.errorTitle}</h1><p>${t.errorText}</p>`
          )
        );
    }
  }
);
