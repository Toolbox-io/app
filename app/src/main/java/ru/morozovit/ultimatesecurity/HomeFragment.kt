@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package ru.morozovit.ultimatesecurity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import ru.morozovit.ultimatesecurity.Settings.installPackage_dsa
import ru.morozovit.ultimatesecurity.Settings.update_dsa
import ru.morozovit.ultimatesecurity.UpdateCheckerService.Companion.checkForUpdates
import ru.morozovit.ultimatesecurity.databinding.HomeBinding
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL


class HomeFragment : Fragment() {
    private lateinit var binding: HomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("StaticFieldLeak")
    inner class UpdateDownloader: AsyncTask<String, String, Unit>() {
        private lateinit var file: File
        private lateinit var mime: String

        override fun doInBackground(vararg params: String?) {
            var count: Int
            try {
                val url = URL(params[0])
                val connection = url.openConnection()
                connection.connect()
                mime = connection.contentType

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                val lengthOfFile = connection.contentLength

                // download the file
                val input: InputStream = BufferedInputStream(
                    url.openStream(),
                    8192
                )

                // Output stream
                file = File(requireActivity().cacheDir.absolutePath + "/update.apk")
                if (file.exists()) {
                    file.delete()
                }
                file.createNewFile()

                val output: OutputStream = FileOutputStream(file)
                val data = ByteArray(1024)
                var total: Long = 0

                while ((input.read(data).also { count = it }) != -1) {
                    total += count.toLong()
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + ((total * 100) / lengthOfFile).toInt())

                    // writing data to file
                    output.write(data, 0, count)
                }

                // flushing output
                output.flush()

                // closing streams
                output.close()
                input.close()
            } catch (e: java.lang.Exception) {
                Log.e("Error: ", e.message!!)
            }
        }

        override fun onProgressUpdate(vararg progress: String) {
            // setting progress percentage
            requireActivity().findViewById<LinearProgressIndicator>(R.id.progress).setProgress(progress[0].toInt())
        }

        override fun onPostExecute(result: Unit?) {
            val progress = requireActivity().findViewById<LinearProgressIndicator>(R.id.progress)
            progress.visibility = GONE
            progress.progress = 0
            if (!installPackage_dsa) {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle("Install package")
                    .setMessage(
                        "The new version is ready to be installed. Install?\n" +
                                "Please note that you will need to grant the \"Install unknown apps\" " +
                                "permission to this app in order to continue."
                    )
                    .setPositiveButton(R.string.install) { _, _ ->
                        val install = Intent(Intent.ACTION_INSTALL_PACKAGE)
                        install.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        install.data = FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().applicationContext.packageName + ".provider",
                            file
                        )
                        startActivity(install)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.dsa) { _, _ ->
                        installPackage_dsa = true
                    }
                    .show()
            } else {
                val install = Intent(Intent.ACTION_INSTALL_PACKAGE)
                install.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                install.data = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().applicationContext.packageName + ".provider",
                    file
                )
                startActivity(install)
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class UpdateChecker: AsyncTask<Any, Unit, Unit>() {
        override fun doInBackground(vararg args: Any? /* binding: HomeBinding, context: Activity */) {
            val binding: HomeBinding = args[0] as HomeBinding
            val context: Activity = args[1] as Activity

            with (context) {
                try {
                    val info = checkForUpdates()

                    // Display the update available
                    if (info != null && info.available) {
                        val cardText =
                            "${info.version.major}." +
                                    "${info.version.minor}." +
                                    "${info.version.patch}"

                        binding.root.post {
                            binding.updateCard.visibility = VISIBLE
                            binding.updateCardVersion.text =
                                String.format(
                                    resources.getString(R.string.update_version),
                                    cardText
                                )
                            binding.updateCardBody.text = info.description
                            binding.updateCardDownload.setOnClickListener {
                                val progress = findViewById<LinearProgressIndicator>(R.id.progress)
                                progress.visibility = VISIBLE
                                UpdateDownloader().execute(info.download)
                            }
                        }
                    } else {}
                } catch (e: Exception) {
                    binding.root.post {
                        Snackbar.make(
                            binding.root,
                            R.string.failed_to_check_for_updates,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!update_dsa) UpdateChecker().execute(binding, requireActivity())
        binding.updateCardDsa.setOnClickListener {
            update_dsa = true
            binding.updateCard.visibility = GONE
        }
    }
}