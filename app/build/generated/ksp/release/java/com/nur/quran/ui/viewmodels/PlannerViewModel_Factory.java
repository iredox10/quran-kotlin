package com.nur.quran.ui.viewmodels;

import com.nur.quran.data.repository.QuranRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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

  public PlannerViewModel_Factory(Provider<QuranRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public PlannerViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static PlannerViewModel_Factory create(Provider<QuranRepository> repositoryProvider) {
    return new PlannerViewModel_Factory(repositoryProvider);
  }

  public static PlannerViewModel newInstance(QuranRepository repository) {
    return new PlannerViewModel(repository);
  }
}
