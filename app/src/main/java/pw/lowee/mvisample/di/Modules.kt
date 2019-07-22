package pw.lowee.mvisample.di

import org.koin.dsl.module
import pw.lowee.mvisample.BuildConfig
import pw.lowee.mvisample.data.ApiClient
import pw.lowee.mvisample.data.DataSource
import pw.lowee.mvisample.data.DataSourceImpl
import pw.lowee.mvisample.data.buildClient
import pw.lowee.mvisample.ui.common.ImageLoader
import pw.lowee.mvisample.ui.common.ImageLoaderImpl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.terrakok.cicerone.Cicerone
import ru.terrakok.cicerone.Router

@Suppress("USELESS_CAST")
val mainModule = module {
    single { buildClient() }
    single {
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.API_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ApiClient::class.java)
    }
    single { DataSourceImpl(get()) as DataSource }
    single { Cicerone.create() }
    single { get<Cicerone<Router>>().navigatorHolder }
    single { get<Cicerone<Router>>().router as Router }
    factory { ImageLoaderImpl() as ImageLoader }
}