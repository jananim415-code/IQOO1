package com.safepayshield.app.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.safepayshield.app.databinding.FragmentFamilyBinding;
import com.safepayshield.app.ui.ElderModeHelper;
import com.safepayshield.app.ui.adapters.ContactAdapter;
import com.safepayshield.app.ui.main.MainViewModel;

public final class FamilyFragment extends Fragment {

    private FragmentFamilyBinding binding;
    private MainViewModel viewModel;
    private ContactAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFamilyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ElderModeHelper.apply(requireContext(), view);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        adapter = new ContactAdapter(contact -> viewModel.removeContact(contact));
        binding.contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.contactsRecyclerView.setAdapter(adapter);

        viewModel.contacts.observe(getViewLifecycleOwner(), contacts -> {
            adapter.setItems(contacts);
            binding.contactsEmptyText.setVisibility(contacts.isEmpty() ? View.VISIBLE : View.GONE);
            binding.contactsRecyclerView.setVisibility(contacts.isEmpty() ? View.GONE : View.VISIBLE);
        });

        binding.btnAddContact.setOnClickListener(v -> showAddContactDialog());
        
        binding.btnTestEscalation.setOnClickListener(v -> {
            viewModel.approveFamily();
            new AlertDialog.Builder(requireContext())
                .setTitle("Test Escalation")
                .setMessage("Notification sent to your trusted contacts.")
                .setPositiveButton("OK", null)
                .show();
        });
    }

    private void showAddContactDialog() {
        EditText input = new EditText(requireContext());
        new AlertDialog.Builder(requireContext())
            .setTitle("Add Trusted Contact")
            .setMessage("Enter name or VPA")
            .setView(input)
            .setPositiveButton("Add", (d, w) -> viewModel.addContact(input.getText().toString()))
            .setNegativeButton("Cancel", null)
            .show();
    }
}
