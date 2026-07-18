package com.nur.quran.ui.viewmodels;

import android.content.Context;
import com.nur.quran.data.repository.QuranRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<QuranRepository> repositoryProvider;

  private final Provider<Context> contextProvider;

  public HomeViewModel_Factory(Provider<QuranRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(repositoryProvider.get(), contextProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<QuranRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    return new HomeViewModel_Factory(repositoryProvider, contextProvider);
  }

  public static HomeViewModel newInstance(QuranRepository repository, Context context) {
    return new HomeViewModel(repository, context);
  }
}
