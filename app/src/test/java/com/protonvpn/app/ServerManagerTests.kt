package com.protonvpn.app

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.protonvpn.android.ProtonApplication
import com.protonvpn.android.models.config.UserData
import com.protonvpn.android.models.vpn.Server
import com.protonvpn.android.utils.Log
import com.protonvpn.android.utils.ServerManager
import com.protonvpn.android.utils.Storage
import com.protonvpn.app.mocks.MockSharedPreference
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import java.io.File


class ServerManagerTests {

    private lateinit var manager: ServerManager

    @get:Rule
    var rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        Storage.setPreferences(MockSharedPreference())
        val userData = mockk<UserData>(relaxed = true)
        val contextMock = mockk<Context>(relaxed = true)
        mockkStatic(ProtonApplication::class)
        mockkStatic(Log::class)
        every { Log.e(any())} answers {}
        every { ProtonApplication.getAppContext() } returns contextMock
        every { userData.hasAccessToServer(any()) } returns true
        every { userData.hasAccessToAnyServer(any()) } returns true
        manager = ServerManager(contextMock, userData)
        val serversFile = File(javaClass.getResource("/Servers.json")?.path)
        val mapper =
                ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val list =
                mapper.readValue(serversFile.readText(), object : TypeReference<List<Server>>() {})

        manager.setServers(list)
    }

    @Test
    fun doNotChooseOfflineServerFromCountry() {
        val country = manager.getVpnCountry("CA", false)
        val countryBestServer = manager.getBestScoreServer(country)
        Assert.assertEquals("CA#2", countryBestServer.serverName)
    }

    @Test
    fun doNotChooseOfflineServerFromAll() {
        Assert.assertEquals("DE#1", manager.bestScoreServerFromAll.serverName)
    }
}

