package com.nur.quran.di;

import android.content.Context;
import com.nur.quran.data.db.QuranDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideQuranDatabaseFactory implements Factory<QuranDatabase> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideQuranDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public QuranDatabase get() {
    return provideQuranDatabase(contextProvider.get());
  }

  public static AppModule_ProvideQuranDatabaseFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideQuranDatabaseFactory(contextProvider);
  }

  public static QuranDatabase provideQuranDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideQuranDatabase(context));
  }
}
