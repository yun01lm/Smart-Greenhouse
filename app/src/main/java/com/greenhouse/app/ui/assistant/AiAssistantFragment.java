package com.greenhouse.app.ui.assistant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.greenhouse.app.databinding.FragmentAiAssistantBinding;
import com.greenhouse.app.ui.diagnosis.DiagnosisFragment;
import com.greenhouse.app.ui.qa.QaFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * AI助手容器 Fragment
 * <p>
 * 内嵌诊断 + 问答两个子页，通过 TabLayout + ViewPager2 切换。
 * 符合规范：Fragment 只负责导航，不写业务逻辑。
 * </p>
 */
public class AiAssistantFragment extends Fragment {

    private FragmentAiAssistantBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiAssistantBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewPager2 适配器
        AssistantPagerAdapter adapter = new AssistantPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);

        // TabLayout 与 ViewPager2 联动
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("病虫害诊断");
                    } else {
                        tab.setText("AI智能问答");
                    }
                }).attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * ViewPager2 适配器：诊断页 + 问答页
     */
    private static class AssistantPagerAdapter extends FragmentStateAdapter {

        public AssistantPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new DiagnosisFragment();
            } else {
                return new QaFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
