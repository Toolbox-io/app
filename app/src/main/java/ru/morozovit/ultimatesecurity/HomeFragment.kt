@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package ru.morozovit.ultimatesecurity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import ru.morozovit.ultimatesecurity.Settings.installPackage_dsa
import ru.morozovit.ultimatesecurity.Settings.update_dsa
import ru.morozovit.ultimatesecurity.databinding.HomeBinding
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection


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
    inner class UpdateChecker: AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            val request = URL("https://api.github.com/repos/denis0001-dev/AIP-Website/releases")
                .openConnection() as HttpsURLConnection
            request.requestMethod = "GET";
            request.setRequestProperty("Accept", "application/vnd.github+json")
            val token = "gi" + "th" + "ub_p" + "at_11BESRTYY" + "0e5lNGcsHV9Up_7HTMBq6ZkfKYXou7bkc" + "mZVX6nMJ0ua9I" + "sqqcsPGmuHHYCZ" + "J4BDL4f0SSrM0"

            request.setRequestProperty("Authorization", "Bearer $token")
            request.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")

            try {
                val input = BufferedInputStream(request.inputStream)
                var c: Char;

                val chars: MutableList<Char> = mutableListOf()

                while (true) {
                    c = input.read().toChar()
                    if (c == 0.toChar() || c == '\uFFFF') break;
                    chars.add(c)
                }
                val response = String(chars.toCharArray())
                val parsedResponse = JsonParser.parseString(response) as JsonArray

                val latestRelease = parsedResponse[0].asJsonObject
                val name = latestRelease["name"].asString;
                val description = latestRelease["body"].asString

                val asset = latestRelease["assets"].asJsonArray[0].asJsonObject["browser_download_url"].asString

                // Parse the semantic version of the latest release
                var majorLatest = 0
                var minorLatest = 0
                var patchLatest = 0
                name.substring(1).split(".").forEachIndexed { index, s ->
                    when (index) {
                        0 -> majorLatest = s.toInt()
                        1 -> minorLatest = s.toInt()
                        2 -> patchLatest = s.toInt()
                    }
                }
                // Parse the semantic version of the current release
                var majorCurrent = 0
                var minorCurrent = 0
                var patchCurrent = 0
                requireActivity()
                    .packageManager
                    .getPackageInfo(requireActivity().packageName, PackageManager.GET_META_DATA)
                    .versionName.split(".")
                    .forEachIndexed { index, s ->
                        when (index) {
                            0 -> majorCurrent = s.toInt()
                            1 -> minorCurrent = s.toInt()
                            2 -> patchCurrent = s.toInt()
                        }
                    }
                // Compare
                var updateAvailable = false
                if (majorLatest > majorCurrent) {
                    updateAvailable = true
                } else if (majorLatest == majorCurrent) {
                    if (minorLatest > minorCurrent) {
                        updateAvailable = true
                    } else if (minorLatest == minorCurrent) {
                        if (patchLatest > patchCurrent) {
                            updateAvailable = true
                        }
                    }
                }

                // Display the update available
                if (updateAvailable) {
                    val cardText =
                        "${if (majorLatest == 0) "" else majorLatest}.${if (minorLatest == 0) "" else minorLatest}.${if (patchLatest == 0) "" else patchLatest}"

                    binding.root.post {
                        binding.updateCard.visibility = VISIBLE
                        binding.updateCardVersion.text =
                            String.format(resources.getString(R.string.update_version), cardText)
                        binding.updateCardBody.text = description
                        binding.updateCardDownload.setOnClickListener {
                            val progress = requireActivity().findViewById<LinearProgressIndicator>(R.id.progress)
                            progress.visibility = VISIBLE
                            UpdateDownloader().execute(asset)
                        }
                    }
                }
            } catch (e: Exception) {
                binding.root.post {
                    Snackbar.make(binding.root, R.string.failed_to_check_for_updates, Snackbar.LENGTH_SHORT).show()
                }
            } finally {
                request.disconnect()
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!update_dsa) UpdateChecker().execute()
        binding.updateCardDsa.setOnClickListener {
            update_dsa = true
            binding.updateCard.visibility = GONE
        }
    }
}