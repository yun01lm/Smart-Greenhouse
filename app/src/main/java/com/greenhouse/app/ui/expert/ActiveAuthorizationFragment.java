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

import com.greenhouse.app.adapter.AuthorizationAdapter;
import com.greenhouse.app.data.model.AuthorizationInfo;
import com.greenhouse.app.viewmodel.ExpertViewModel;

/**
 * 已授权 Fragment (F10)
 */
public class ActiveAuthorizationFragment extends Fragment {

    private ExpertViewModel viewModel;
    private AuthorizationAdapter adapter;

    public static ActiveAuthorizationFragment newInstance() {
        return new ActiveAuthorizationFragment();
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

        adapter = new AuthorizationAdapter(false); // 已授权模式
        adapter.setOnAuthorizationActionListener(new AuthorizationAdapter.OnAuthorizationActionListener() {
            @Override
            public void onApprove(AuthorizationInfo auth) {
                // 已授权模式不处理同意
            }

            @Override
            public void onReject(AuthorizationInfo auth) {
                // 已授权模式不处理拒绝
            }

            @Override
            public void onRevoke(AuthorizationInfo auth) {
                viewModel.revokeAuthorization(auth.getId());
                Toast.makeText(requireContext(), "已撤销授权", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        viewModel.getActiveAuthorizations().observe(getViewLifecycleOwner(), authorizations -> {
            adapter.setData(authorizations);
        });
    }
}
