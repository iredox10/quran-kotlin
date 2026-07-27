package com.nur.quran.di;

import com.nur.quran.data.db.QuranDatabase;
import com.nur.quran.data.db.dao.QuranDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideQuranDaoFactory implements Factory<QuranDao> {
  private final Provider<QuranDatabase> databaseProvider;

  public AppModule_ProvideQuranDaoFactory(Provider<QuranDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public QuranDao get() {
    return provideQuranDao(databaseProvider.get());
  }

  public static AppModule_ProvideQuranDaoFactory create(Provider<QuranDatabase> databaseProvider) {
    return new AppModule_ProvideQuranDaoFactory(databaseProvider);
  }

  public static QuranDao provideQuranDao(QuranDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideQuranDao(database));
  }
}
