package com.marcfradera.shooterranking.ui.fragments

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.databinding.FragmentRecyclerScreenBinding
import com.marcfradera.shooterranking.shared.NavigationSharedViewModel
import com.marcfradera.shooterranking.ui.adapters.EquipsAdapter
import com.marcfradera.shooterranking.ui.viewmodel.EquipsLiveDataViewModel

class EquipsFragment : Fragment(R.layout.fragment_recycler_screen) {

    private var _binding: FragmentRecyclerScreenBinding? = null
    private val binding get() = _binding!!

    private val vm by viewModels<EquipsLiveDataViewModel>()
    private val shared by activityViewModels<NavigationSharedViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRecyclerScreenBinding.bind(view)

        val adapter = EquipsAdapter(
            onClick = {
                shared.setEquip(it.equip.id_equip, it.equip.nom_equip)
                findNavController().navigate(R.id.action_equips_to_ranking)
            },
            onEdit = {
                showEditEquipDialog(
                    idEquip = it.equip.id_equip,
                    initialNomEquip = it.equip.nom_equip,
                    initialTipusPista = it.equip.tipus_pista
                )
            },
            onDelete = {
                showDeleteEquipDialog(
                    idEquip = it.equip.id_equip,
                    nomEquip = it.equip.nom_equip
                )
            }
        )

        val temporadaLabel = shared.selection.value?.temporadaLabel.orEmpty()
        binding.titleText.text =
            if (temporadaLabel.isBlank()) "EQUIPS"
            else "EQUIPS: $temporadaLabel"

        binding.backButton.visibility = View.VISIBLE
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.settingsButton.setOnClickListener {
            showSettingsDialog()
        }

        binding.primaryButton.text = "Afegir equip"
        binding.primaryButton.isEnabled = true
        binding.subtitleText.visibility = View.GONE

        binding.primaryButton.setOnClickListener {
            showCreateEquipDialog()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        vm.state.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.data)
            state.error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        val temporadaId = shared.selection.value?.temporadaId.orEmpty()
        if (temporadaId.isNotBlank()) {
            vm.load(temporadaId)
        }
    }

    private fun showSettingsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Configuració")
            .setItems(arrayOf("Tancar sessió")) { _, which ->
                if (which == 0) {
                    logoutAndRestart()
                }
            }
            .setNegativeButton("Cancel·lar", null)
            .show()
    }

    private fun logoutAndRestart() {
        FirebaseAuth.getInstance().signOut()

        val launchIntent = requireContext().packageManager
            .getLaunchIntentForPackage(requireContext().packageName)

        launchIntent?.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )

        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            requireActivity().recreate()
        }
    }

    private fun showCreateEquipDialog() {
        val temporadaId = shared.selection.value?.temporadaId.orEmpty()
        if (temporadaId.isBlank()) {
            Toast.makeText(requireContext(), "No hi ha temporada seleccionada", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val context = requireContext()
        var tipusPistaSeleccionat = "Base"

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, 8, pad, 0)
        }

        val nomEdit = EditText(context).apply {
            hint = "Nom de l'equip"
        }

        val tipusPistaView = createTipusPistaSelector(
            initialTipusPista = "Base",
            enabled = true,
            onSelected = { tipusPistaSeleccionat = it }
        )

        container.addView(nomEdit)
        container.addView(tipusPistaView)

        MaterialAlertDialogBuilder(context)
            .setTitle("Afegir equip")
            .setView(container)
            .setNegativeButton("Cancel·lar", null)
            .setPositiveButton("Guardar", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nom = nomEdit.text.toString().trim()

                        if (nom.isBlank()) {
                            nomEdit.error = "Introdueix un nom"
                            return@setOnClickListener
                        }

                        vm.create(
                            temporadaId = temporadaId,
                            nomEquip = nom,
                            tipusPista = tipusPistaSeleccionat,
                            onDone = { dialog.dismiss() },
                            onError = {
                                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                dialog.show()
            }
    }

    private fun showEditEquipDialog(
        idEquip: String,
        initialNomEquip: String,
        initialTipusPista: String
    ) {
        val temporadaId = shared.selection.value?.temporadaId.orEmpty()
        if (temporadaId.isBlank()) {
            Toast.makeText(requireContext(), "No hi ha temporada seleccionada", Toast.LENGTH_SHORT)
                .show()
            return
        }

        vm.loadDeletePreview(
            idEquip = idEquip,
            onDone = { preview ->
                val hasSessions = preview.sessionsCount > 0

                showEditEquipDialogContent(
                    idEquip = idEquip,
                    temporadaId = temporadaId,
                    initialNomEquip = initialNomEquip,
                    initialTipusPista = initialTipusPista,
                    canEditTipusPista = !hasSessions
                )
            },
            onError = {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showEditEquipDialogContent(
        idEquip: String,
        temporadaId: String,
        initialNomEquip: String,
        initialTipusPista: String,
        canEditTipusPista: Boolean
    ) {
        val context = requireContext()
        var tipusPistaSeleccionat = initialTipusPista.ifBlank { "Base" }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, 8, pad, 0)
        }

        val nomEdit = EditText(context).apply {
            hint = "Nom de l'equip"
            setText(initialNomEquip)
            setSelection(text.length)
        }

        val tipusPistaView = createTipusPistaSelector(
            initialTipusPista = tipusPistaSeleccionat,
            enabled = canEditTipusPista,
            onSelected = { tipusPistaSeleccionat = it }
        )

        container.addView(nomEdit)
        container.addView(tipusPistaView)

        if (!canEditTipusPista) {
            val warningText = TextView(context).apply {
                text = "No es pot canviar el tipus de pista perquè aquest equip ja té sessions de tir registrades."
                textSize = 13f
                val topMargin = (8 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, topMargin, 0, 0)
                }
            }

            container.addView(warningText)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Editar equip")
            .setView(container)
            .setNegativeButton("Cancel·lar", null)
            .setPositiveButton("Guardar", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nom = nomEdit.text.toString().trim()

                        if (nom.isBlank()) {
                            nomEdit.error = "Introdueix un nom"
                            return@setOnClickListener
                        }

                        vm.update(
                            idEquip = idEquip,
                            temporadaId = temporadaId,
                            nomEquip = nom,
                            tipusPista = tipusPistaSeleccionat,
                            onDone = { dialog.dismiss() },
                            onError = {
                                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                dialog.show()
            }
    }

    private fun createTipusPistaSelector(
        initialTipusPista: String,
        enabled: Boolean,
        onSelected: (String) -> Unit
    ): View {
        val context = requireContext()
        val density = resources.displayMetrics.density

        val wrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val title = TextView(context).apply {
            text = "Tipus de pista"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            val topMargin = (18 * density).toInt()
            val bottomMargin = (8 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, topMargin, 0, bottomMargin)
            }
        }

        val toggleGroup = MaterialButtonToggleGroup(context).apply {
            orientation = LinearLayout.VERTICAL
            isSingleSelection = true
            isSelectionRequired = true
            isEnabled = enabled
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val baseButtonId = View.generateViewId()
        val amateurButtonId = View.generateViewId()
        val proButtonId = View.generateViewId()

        fun createButton(idValue: Int, label: String): MaterialButton {
            return MaterialButton(
                context,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                id = idValue
                text = label
                isCheckable = true
                isEnabled = enabled
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }

        val baseButton = createButton(baseButtonId, "Base")
        val amateurButton = createButton(amateurButtonId, "Amateur")
        val proButton = createButton(proButtonId, "Pro")

        toggleGroup.addView(baseButton)
        toggleGroup.addView(amateurButton)
        toggleGroup.addView(proButton)

        val selectedId = when (initialTipusPista) {
            "Amateur" -> amateurButtonId
            "Pro" -> proButtonId
            else -> baseButtonId
        }

        toggleGroup.check(selectedId)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            val selected = when (checkedId) {
                baseButtonId -> "Base"
                amateurButtonId -> "Amateur"
                proButtonId -> "Pro"
                else -> "Base"
            }

            onSelected(selected)
        }

        wrapper.addView(title)
        wrapper.addView(toggleGroup)

        return wrapper
    }

    private fun showDeleteEquipDialog(idEquip: String, nomEquip: String) {
        val temporadaId = shared.selection.value?.temporadaId.orEmpty()

        vm.loadDeletePreview(
            idEquip = idEquip,
            onDone = { preview ->
                val message = buildString {
                    append("Vols eliminar l'equip \"$nomEquip\"?\n\n")
                    append("S'eliminarà el següent:\n")
                    append("Jugadores: ${preview.jugadors.size}\n")
                    append("Sessions totals: ${preview.sessionsCount}\n\n")
                    append("Aquesta acció no es pot desfer.")
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Confirmar eliminació")
                    .setMessage(message)
                    .setNegativeButton("Cancel·lar", null)
                    .setPositiveButton("Eliminar") { _, _ ->
                        vm.delete(
                            idEquip = idEquip,
                            temporadaId = temporadaId,
                            onDone = {
                                Toast.makeText(
                                    requireContext(),
                                    "Equip eliminat",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onError = {
                                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    .show()
            },
            onError = {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}