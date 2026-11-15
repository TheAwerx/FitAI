package com.fitai.app.di

import com.fitai.app.data.DatabaseDriverFactory
import org.koin.dsl.module

expect fun platformDriverModule(): org.koin.core.module.Module
