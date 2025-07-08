package com.example.invoicesaver

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.invoicesaver.ui.theme.InvoicesaverTheme
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.edit
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

private const val PREFS_NAME = "invoice_prefs"
private const val KEY_IMAGE_URIS = "image_uris"

fun saveImageUri(context: Context, uri: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val set = prefs.getStringSet(KEY_IMAGE_URIS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    set.add(uri)
    prefs.edit { putStringSet(KEY_IMAGE_URIS, set) }
}

fun removeImageUri(context: Context, uri: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val set = prefs.getStringSet(KEY_IMAGE_URIS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    set.remove(uri)
    prefs.edit { putStringSet(KEY_IMAGE_URIS, set) }
}

fun getImageUris(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_IMAGE_URIS, emptySet())?.toList() ?: emptyList()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InvoicesaverTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") { MainScreen(navController) }
                    composable("gallery") { GalleryScreen(navController) }
                    composable("detail/{imageUri}") { backStackEntry ->
                        val imageUri = backStackEntry.arguments?.getString("imageUri")
                        DetailScreen(imageUri, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavHostController) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveImageUri(context, it.toString())
            Toast.makeText(context, "Invoice uploaded!", Toast.LENGTH_SHORT).show()
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Invoice Saver") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                launcher.launch("image/*")
            }) {
                Icon(Icons.Default.Add, contentDescription = "Upload")
                Text("Upload Invoice")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("gallery") }) {
                Icon(Icons.Default.Photo, contentDescription = "Gallery")
                Text("Gallery")
            }
        }
    }
}

@Composable
fun GalleryScreen(navController: NavHostController) {
    val context = LocalContext.current
    var imageUris by remember { mutableStateOf(getImageUris(context)) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Gallery") }) }
    ) { innerPadding ->
        if (imageUris.isEmpty()) {
            Text(
                text = "No invoices uploaded yet.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(imageUris) { uri ->
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { navController.navigate("detail/${Uri.encode(uri)}") }
                            .padding(8.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "Invoice thumbnail",
                            modifier = Modifier.height(64.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier
                                .clickable {
                                    removeImageUri(context, uri)
                                    imageUris = getImageUris(context)
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(imageUri: String?, navController: NavHostController) {
    val context = LocalContext.current
    if (imageUri == null) {
        Text("No image found.")
        return
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Invoice Detail") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(Uri.decode(imageUri)),
                contentDescription = "Invoice image",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                removeImageUri(context, Uri.decode(imageUri))
                navController.popBackStack()
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
                Text("Delete Invoice")
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    InvoicesaverTheme {
        Greeting("Android")
    }
}