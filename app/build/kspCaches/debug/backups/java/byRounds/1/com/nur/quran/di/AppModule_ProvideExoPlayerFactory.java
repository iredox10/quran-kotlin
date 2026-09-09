package com.nur.quran.di;

import androidx.media3.exoplayer.ExoPlayer;
import com.nur.quran.data.audio.AudioPlayerHolder;
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
public final class AppModule_ProvideExoPlayerFactory implements Factory<ExoPlayer> {
  private final Provider<AudioPlayerHolder> holderProvider;

  public AppModule_ProvideExoPlayerFactory(Provider<AudioPlayerHolder> holderProvider) {
    this.holderProvider = holderProvider;
  }

  @Override
  public ExoPlayer get() {
    return provideExoPlayer(holderProvider.get());
  }

  public static AppModule_ProvideExoPlayerFactory create(
      Provider<AudioPlayerHolder> holderProvider) {
    return new AppModule_ProvideExoPlayerFactory(holderProvider);
  }

  public static ExoPlayer provideExoPlayer(AudioPlayerHolder holder) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideExoPlayer(holder));
  }
}
