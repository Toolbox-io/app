@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package ru.morozovit.android.utils

import android.os.AsyncTask

abstract class SimpleAsyncTask: AsyncTask<Unit, Unit, Unit>() {
    final override fun doInBackground(vararg params: Unit) = run()
    abstract fun run()
    abstract fun postRun()
    final override fun onProgressUpdate(vararg values: Unit) {}
    final override fun onPostExecute(result: Unit) = postRun()
}