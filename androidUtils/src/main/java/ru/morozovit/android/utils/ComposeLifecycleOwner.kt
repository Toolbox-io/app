package ru.morozovit.android.utils

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

class ComposeLifecycleOwner : SavedStateRegistryOwner, LifecycleOwner {
    private var mLifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private var mSavedStateRegistryController: SavedStateRegistryController = SavedStateRegistryController.Companion.create(this)

    fun onCreate() {
        performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycle.currentState = Lifecycle.State.CREATED
    }

    fun onStart() {
        handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycle.currentState = Lifecycle.State.STARTED
    }

    fun onDestroy() {
        handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycle.currentState = Lifecycle.State.DESTROYED
    }

    override val lifecycle = mLifecycleRegistry

    fun setCurrentState(state: Lifecycle.State) {
        mLifecycleRegistry.currentState = state
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        mLifecycleRegistry.handleLifecycleEvent(event)
    }

    override val savedStateRegistry: SavedStateRegistry
        get() = mSavedStateRegistryController.savedStateRegistry

    fun performRestore(savedState: Bundle?) {
        mSavedStateRegistryController.performRestore(savedState)
    }

    fun performSave(outBundle: Bundle) {
        mSavedStateRegistryController.performSave(outBundle)
    }
}