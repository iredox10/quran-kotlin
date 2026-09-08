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
public final class PlannerViewModel_Factory implements Factory<PlannerViewModel> {
  private final Provider<QuranRepository> repositoryProvider;

  private final Provider<Context> appContextProvider;

  public PlannerViewModel_Factory(Provider<QuranRepository> repositoryProvider,
      Provider<Context> appContextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.appContextProvider = appContextProvider;
  }

  @Override
  public PlannerViewModel get() {
    return newInstance(repositoryProvider.get(), appContextProvider.get());
  }

  public static PlannerViewModel_Factory create(Provider<QuranRepository> repositoryProvider,
      Provider<Context> appContextProvider) {
    return new PlannerViewModel_Factory(repositoryProvider, appContextProvider);
  }

  public static PlannerViewModel newInstance(QuranRepository repository, Context appContext) {
    return new PlannerViewModel(repository, appContext);
  }
}
