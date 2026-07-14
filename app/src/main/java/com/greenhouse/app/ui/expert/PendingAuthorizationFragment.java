package com.greenhouse.app.ui.expert;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.R;
import com.greenhouse.app.adapter.AuthorizationAdapter;
import com.greenhouse.app.data.model.AuthorizationInfo;
import com.greenhouse.app.viewmodel.ExpertViewModel;

/**
 * 待处理授权 Fragment (F10)
 */
public class PendingAuthorizationFragment extends Fragment {

    private ExpertViewModel viewModel;
    private AuthorizationAdapter adapter;

    public static PendingAuthorizationFragment newInstance() {
        return new PendingAuthorizationFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        recyclerView.setPadding(0, 8, 0, 0);
        return recyclerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ExpertViewModel.class);

        RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new AuthorizationAdapter(true); // 待处理模式
        adapter.setOnAuthorizationActionListener(new AuthorizationAdapter.OnAuthorizationActionListener() {
            @Override
            public void onApprove(AuthorizationInfo auth) {
                viewModel.approveAuthorization(auth.getId());
                Toast.makeText(requireContext(), "已同意授权", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReject(AuthorizationInfo auth) {
                viewModel.rejectAuthorization(auth.getId());
                Toast.makeText(requireContext(), "已拒绝授权", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRevoke(AuthorizationInfo auth) {
                // 待处理模式不处理撤销
            }
        });
        recyclerView.setAdapter(adapter);

        viewModel.getPendingAuthorizations().observe(getViewLifecycleOwner(), authorizations -> {
            adapter.setData(authorizations);
        });
    }
}
