package com.nur.quran.data.repository;

import com.google.gson.Gson;
import com.nur.quran.data.api.QuranApi;
import com.nur.quran.data.db.dao.QuranDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class QuranRepository_Factory implements Factory<QuranRepository> {
  private final Provider<QuranDao> quranDaoProvider;

  private final Provider<QuranApi> quranApiProvider;

  private final Provider<Gson> gsonProvider;

  public QuranRepository_Factory(Provider<QuranDao> quranDaoProvider,
      Provider<QuranApi> quranApiProvider, Provider<Gson> gsonProvider) {
    this.quranDaoProvider = quranDaoProvider;
    this.quranApiProvider = quranApiProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public QuranRepository get() {
    return newInstance(quranDaoProvider.get(), quranApiProvider.get(), gsonProvider.get());
  }

  public static QuranRepository_Factory create(Provider<QuranDao> quranDaoProvider,
      Provider<QuranApi> quranApiProvider, Provider<Gson> gsonProvider) {
    return new QuranRepository_Factory(quranDaoProvider, quranApiProvider, gsonProvider);
  }

  public static QuranRepository newInstance(QuranDao quranDao, QuranApi quranApi, Gson gson) {
    return new QuranRepository(quranDao, quranApi, gson);
  }
}
