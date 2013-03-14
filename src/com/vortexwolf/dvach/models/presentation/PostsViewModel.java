package com.vortexwolf.dvach.models.presentation;

import java.util.HashMap;

import android.content.res.Resources.Theme;

import com.vortexwolf.dvach.interfaces.IURLSpanClickListener;
import com.vortexwolf.dvach.models.domain.PostInfo;
import com.vortexwolf.dvach.services.presentation.DvachUriBuilder;
import com.vortexwolf.dvach.settings.ApplicationSettings;

public class PostsViewModel {

    private final HashMap<String, PostItemViewModel> mViewModels = new HashMap<String, PostItemViewModel>();
    private String mLastPostNumber = null;

    // Обновляет ссылки у других постов и добавляет модель в список
    private void ProcessPostItem(PostItemViewModel viewModel) {
        for (String refPostNumber : viewModel.getRefersTo()) {
            PostItemViewModel refModel = this.mViewModels.get(refPostNumber);
            if (refModel != null) {
                refModel.addReferenceFrom(viewModel.getNumber());
            }
        }

        this.mViewModels.put(viewModel.getNumber(), viewModel);
    }

    public PostItemViewModel getModel(String postNumber) {
        return this.mViewModels.get(postNumber);
    }

    /**
     * Создает view model на основе модели
     * 
     * @param item
     *            Модель какого-нибудь сообщения в треде
     * @param theme
     *            Текущая тема приложения
     * @param listener
     *            Обработчик события нажатия на ссылку в посте
     * @return Созданная view model
     */
    public PostItemViewModel createModel(PostInfo item, Theme theme, ApplicationSettings settings, IURLSpanClickListener listener, DvachUriBuilder uriBuilder) {
        PostItemViewModel viewModel = new PostItemViewModel(this.mViewModels.size(), item, theme, settings, listener, uriBuilder);
        this.mLastPostNumber = viewModel.getNumber();

        this.ProcessPostItem(viewModel);

        return viewModel;
    }

    /**
     * Возвращает номер последнего сообщения в треде
     */
    public String getLastPostNumber() {
        return this.mLastPostNumber;
    }
}
