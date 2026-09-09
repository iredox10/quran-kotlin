package com.nur.quran.data.audio;

import android.content.Context;
import com.nur.quran.data.db.dao.QuranDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class TimingImporter_Factory implements Factory<TimingImporter> {
  private final Provider<Context> contextProvider;

  private final Provider<QuranDao> quranDaoProvider;

  public TimingImporter_Factory(Provider<Context> contextProvider,
      Provider<QuranDao> quranDaoProvider) {
    this.contextProvider = contextProvider;
    this.quranDaoProvider = quranDaoProvider;
  }

  @Override
  public TimingImporter get() {
    return newInstance(contextProvider.get(), quranDaoProvider.get());
  }

  public static TimingImporter_Factory create(Provider<Context> contextProvider,
      Provider<QuranDao> quranDaoProvider) {
    return new TimingImporter_Factory(contextProvider, quranDaoProvider);
  }

  public static TimingImporter newInstance(Context context, QuranDao quranDao) {
    return new TimingImporter(context, quranDao);
  }
}
