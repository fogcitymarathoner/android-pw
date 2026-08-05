package com.example.pw

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pw.ui.theme.PwTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MainActivity : ComponentActivity() {
    private var keepScreenOnJob: Job? = null

    fun keepScreenOnTemporarily(durationMs: Long = 30000) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        keepScreenOnJob?.cancel()
        keepScreenOnJob = lifecycleScope.launch {
            delay(durationMs)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PwTheme {
                val navController = rememberNavController()
                val initialUser = remember { Firebase.auth.currentUser }
                var currentUser by remember { mutableStateOf(initialUser) }

                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { auth ->
                        val user = auth.currentUser
                        if (user?.uid != currentUser?.uid) {
                            currentUser = user
                            if (user != null) {
                                navController.navigate("passwords") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                    Firebase.auth.addAuthStateListener(listener)
                    onDispose { Firebase.auth.removeAuthStateListener(listener) }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (initialUser == null) "auth" else "passwords"
                ) {
                    composable("auth") { AuthScreen() }
                    composable("passwords") {
                        currentUser?.let { user ->
                            MainLayout(user = user, navController) { PasswordsScreen(user, this@MainActivity) }
                        }
                    }
                    composable("expenses") {
                        currentUser?.let { user ->
                            MainLayout(user = user, navController) { ExpensesScreen(user) }
                        }
                    }
                    composable("subscriptions") {
                        currentUser?.let { user ->
                            MainLayout(user = user, navController) { SubscriptionsScreen(user) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainLayout(
    user: FirebaseUser,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Password, contentDescription = null) },
                    label = { Text("Passwords") },
                    selected = currentRoute == "passwords",
                    onClick = { navController.navigate("passwords") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                    label = { Text("Expenses") },
                    selected = currentRoute == "expenses",
                    onClick = { navController.navigate("expenses") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Subscriptions, contentDescription = null) },
                    label = { Text("Subs") },
                    selected = currentRoute == "subscriptions",
                    onClick = { navController.navigate("subscriptions") { launchSingleTop = true } }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content()
        }
    }
}

@Composable
fun AuthScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = androidx.credentials.CredentialManager.create(context)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Personal Assistant", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            scope.launch {
                try {
                    val rawNonce = UUID.randomUUID().toString()
                    val bytes = rawNonce.toByteArray()
                    val md = java.security.MessageDigest.getInstance("SHA-256")
                    val digest = md.digest(bytes)
                    val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                        .setNonce(hashedNonce)
                        .setAutoSelectEnabled(true)
                        .build()

                    val request = androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(context, request)
                    val credential = result.credential

                    val googleIdTokenCredential = try {
                        if (credential is GoogleIdTokenCredential) {
                            credential
                        } else {
                            GoogleIdTokenCredential.createFrom(credential.data)
                        }
                    } catch (e: Exception) { null }

                    if (googleIdTokenCredential != null) {
                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                        Firebase.auth.signInWithCredential(firebaseCredential).await()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }) {
            Text("Sign in with Google")
        }
    }
}

@Composable
fun PasswordsScreen(user: FirebaseUser, activity: MainActivity) {
    val dbRef = Firebase.database.reference.child("users").child(user.uid).child("passwords")
    var passwords by remember { mutableStateOf(listOf<PwEntity>()) }
    var searchQuery by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var entryToEdit by remember { mutableStateOf<PwEntity?>(null) }
    var entryToDelete by remember { mutableStateOf<PwEntity?>(null) }
    var entryToView by remember { mutableStateOf<PwEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    DisposableEffect(user.uid) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<PwEntity>()
                for (child in snapshot.children) {
                    val vendor = child.child("vendor").value?.toString() ?: ""
                    val account = child.child("account").value?.toString() ?: ""
                    val pw = child.child("pw").value?.toString() ?: ""
                    list.add(PwEntity(id = child.key, vendor = vendor, account = account, pw = pw))
                }
                passwords = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        dbRef.addValueEventListener(listener)
        onDispose { dbRef.removeEventListener(listener) }
    }

    val filtered = passwords.filter { 
        it.vendor.contains(searchQuery, ignoreCase = true) || it.account.contains(searchQuery, ignoreCase = true) 
    }

    if (showAddDialog) {
        EntryDialog(title = "Add Password", onDismiss = { showAddDialog = false }, onSave = { v, a, p ->
            dbRef.push().setValue(PwEntity(vendor = v, account = a, pw = p))
            showAddDialog = false
        })
    }

    entryToEdit?.let { entity ->
        EntryDialog(title = "Edit Password", initialVendor = entity.vendor, initialAccount = entity.account, initialPassword = entity.pw,
            onDismiss = { entryToEdit = null }, onSave = { v, a, p ->
                entity.id?.let { dbRef.child(it).setValue(PwEntity(vendor = v, account = a, pw = p)) }
                entryToEdit = null
            }
        )
    }

    entryToDelete?.let { entity ->
        AlertDialog(onDismissRequest = { entryToDelete = null }, title = { Text("Delete Entry?") }, text = { Text("Delete ${entity.vendor}?") },
            confirmButton = { Button(onClick = { entity.id?.let { dbRef.child(it).removeValue() }; entryToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) ) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { entryToDelete = null }) { Text("Cancel") } }
        )
    }

    entryToView?.let { entity ->
        ViewPasswordDialog(entity = entity, onDismiss = { entryToView = null }, onCopy = {
            clipboardManager.setText(AnnotatedString(entity.pw))
            Toast.makeText(context, "${entity.vendor}'s password copied", Toast.LENGTH_SHORT).show()
            activity.keepScreenOnTemporarily()
        })
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Color(0xFF2196F3), contentColor = Color.White, shape = CircleShape) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            TopHeader(user.email ?: "", "My Passwords")
            SearchBar(searchQuery) { searchQuery = it }
            LazyColumn {
                items(filtered, key = { it.id ?: "" }) { item ->
                    PasswordCard(item, 
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(item.pw))
                            Toast.makeText(context, "${item.vendor}'s password copied", Toast.LENGTH_SHORT).show()
                            activity.keepScreenOnTemporarily()
                        },
                        onView = { entryToView = item },
                        onEdit = { entryToEdit = item },
                        onDelete = { entryToDelete = item }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpensesScreen(user: FirebaseUser) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val expenseDao = remember { db.expenseDao() }
    val categoryDao = remember { db.categoryDao() }
    val vendorDao = remember { db.vendorDao() }
    
    val expensesWithDetails by expenseDao.getExpensesWithDetailsForUser(user.uid).collectAsState(initial = emptyList())
    val categories by categoryDao.getCategoriesForUser(user.uid).collectAsState(initial = emptyList())
    val vendors by vendorDao.getVendorsForUser(user.uid).collectAsState(initial = emptyList())
    
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedVendorId by remember { mutableStateOf<String?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<ExpenseWithDetails?>(null) }
    var entryToDelete by remember { mutableStateOf<Expense?>(null) }
    val scope = rememberCoroutineScope()

    val fbExpensesRef = remember(user.uid) { Firebase.database.reference.child("users").child(user.uid).child("expenses") }
    val fbCategoriesRef = remember(user.uid) { Firebase.database.reference.child("users").child(user.uid).child("categories") }
    val fbVendorsRef = remember(user.uid) { Firebase.database.reference.child("users").child(user.uid).child("vendors") }

    // Sync from Firebase to Room
    DisposableEffect(user.uid) {
        val catListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    snapshot.children.forEach { child ->
                        val remoteId = child.key ?: return@forEach
                        val name = child.child("name").value?.toString() ?: ""
                        val existing = categoryDao.getByCategoryRemoteId(remoteId)
                        if (existing == null) {
                            categoryDao.insert(Category(remoteId = remoteId, name = name, userId = user.uid))
                        } else if (existing.name != name) {
                            categoryDao.insert(existing.copy(name = name))
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        fbCategoriesRef.addValueEventListener(catListener)

        val vendorListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    snapshot.children.forEach { child ->
                        val remoteId = child.key ?: return@forEach
                        val name = child.child("name").value?.toString() ?: ""
                        val existing = vendorDao.getByVendorRemoteId(remoteId)
                        if (existing == null) {
                            vendorDao.insert(Vendor(remoteId = remoteId, name = name, userId = user.uid))
                        } else if (existing.name != name) {
                            vendorDao.insert(existing.copy(name = name))
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        fbVendorsRef.addValueEventListener(vendorListener)

        val expListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    snapshot.children.forEach { child ->
                        val remoteId = child.key ?: return@forEach
                        val amount = child.child("amount").value?.toString() ?: ""
                        val date = child.child("date").value?.toString() ?: ""
                        val memo = child.child("memo").value?.toString() ?: ""
                        val remoteCatId = child.child("remoteCategoryId").value?.toString()
                        val remoteVendorId = child.child("remoteVendorId").value?.toString()

                        val existing = expenseDao.getByExpenseRemoteId(remoteId)
                        val newExpense = Expense(
                            remoteId = remoteId,
                            vendorId = remoteVendorId,
                            categoryId = remoteCatId,
                            amount = amount,
                            date = date,
                            memo = memo,
                            userId = user.uid
                        )
                        if (existing == null || existing != newExpense) {
                            expenseDao.insert(newExpense)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        fbExpensesRef.addValueEventListener(expListener)

        onDispose {
            fbCategoriesRef.removeEventListener(catListener)
            fbVendorsRef.removeEventListener(vendorListener)
            fbExpensesRef.removeEventListener(expListener)
        }
    }

    val filtered = remember(expensesWithDetails, selectedCategoryId, selectedVendorId, searchQuery) {
        expensesWithDetails.filter { 
            (selectedCategoryId == null || it.expense.categoryId == selectedCategoryId) &&
            (selectedVendorId == null || it.expense.vendorId == selectedVendorId) &&
            (searchQuery.isBlank() || 
             (it.vendorName ?: "").contains(searchQuery, ignoreCase = true) || 
             (it.categoryName ?: "").contains(searchQuery, ignoreCase = true))
        }.sortedByDescending { it.expense.date }
    }

    // Vendors filtered by selected category for the workflow
    val vendorsForCategory = remember(vendors, selectedCategoryId, expensesWithDetails) {
        if (selectedCategoryId == null) vendors.sortedBy { it.name }
        else {
            val vendorIdsInCategory = expensesWithDetails
                .filter { it.expense.categoryId == selectedCategoryId }
                .mapNotNull { it.expense.vendorId }
                .toSet()
            vendors.filter { it.remoteId in vendorIdsInCategory }.sortedBy { it.name }
        }
    }

    if (showAddDialog) {
        ExpenseDialog(
            initialVendorId = selectedVendorId,
            initialCategoryId = selectedCategoryId,
            categories = categories,
            vendors = vendors,
            onDismiss = { showAddDialog = false },
            onSave = { vendorId, catId, a, d, m ->
                scope.launch {
                    val cat = categories.find { it.remoteId == catId }
                    val ven = vendors.find { it.remoteId == vendorId }
                    val newExpRef = fbExpensesRef.push()
                    val remoteId = newExpRef.key
                    
                    val expense = Expense(
                        remoteId = remoteId!!,
                        vendorId = vendorId, 
                        categoryId = catId, 
                        amount = a, 
                        date = d, 
                        memo = m, 
                        userId = user.uid
                    )
                    expenseDao.insert(expense)
                    newExpRef.setValue(mapOf(
                        "amount" to a,
                        "date" to d,
                        "memo" to m,
                        "remoteCategoryId" to cat?.remoteId,
                        "remoteVendorId" to ven?.remoteId
                    ))
                }
                showAddDialog = false
            },
            onAddCategory = { name ->
                scope.launch {
                    val newCatRef = fbCategoriesRef.push()
                    val rid = newCatRef.key!!
                    categoryDao.insert(Category(remoteId = rid, name = name, userId = user.uid))
                    newCatRef.setValue(mapOf("name" to name))
                }
            },
            onAddVendor = { name ->
                scope.launch {
                    val newVenRef = fbVendorsRef.push()
                    val rid = newVenRef.key!!
                    vendorDao.insert(Vendor(remoteId = rid, name = name, userId = user.uid))
                    newVenRef.setValue(mapOf("name" to name))
                }
            }
        )
    }

    entryToEdit?.let { item ->
        ExpenseDialog(
            initialVendorId = item.expense.vendorId,
            initialCategoryId = item.expense.categoryId,
            initialAmount = item.expense.amount,
            initialDate = item.expense.date,
            initialMemo = item.expense.memo,
            categories = categories,
            vendors = vendors,
            onDismiss = { entryToEdit = null },
            onSave = { venId, catId, a, d, m ->
                scope.launch {
                    val cat = categories.find { it.remoteId == catId }
                    val ven = vendors.find { it.remoteId == venId }
                    val updatedExpense = item.expense.copy(vendorId = venId, categoryId = catId, amount = a, date = d, memo = m)
                    expenseDao.update(updatedExpense)
                    
                    updatedExpense.remoteId?.let { rId ->
                        fbExpensesRef.child(rId).setValue(mapOf(
                            "amount" to a,
                            "date" to d,
                            "memo" to m,
                            "remoteCategoryId" to cat?.remoteId,
                            "remoteVendorId" to ven?.remoteId
                        ))
                    }
                }
                entryToEdit = null
            },
            onAddCategory = { name ->
                scope.launch {
                    val newCatRef = fbCategoriesRef.push()
                    val rid = newCatRef.key!!
                    categoryDao.insert(Category(remoteId = rid, name = name, userId = user.uid))
                    newCatRef.setValue(mapOf("name" to name))
                }
            },
            onAddVendor = { name ->
                scope.launch {
                    val newVenRef = fbVendorsRef.push()
                    val rid = newVenRef.key!!
                    vendorDao.insert(Vendor(remoteId = rid, name = name, userId = user.uid))
                    newVenRef.setValue(mapOf("name" to name))
                }
            }
        )
    }

    entryToDelete?.let { item ->
        AlertDialog(onDismissRequest = { entryToDelete = null }, title = { Text("Delete Expense?") }, text = { Text("Delete expense for ${item.vendorId ?: "item"}?") },
            confirmButton = { Button(onClick = { 
                scope.launch { 
                    expenseDao.delete(item) 
                    item.remoteId?.let { fbExpensesRef.child(it).removeValue() }
                }
                entryToDelete = null 
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) ) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { entryToDelete = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Color(0xFF4CAF50), contentColor = Color.White, shape = CircleShape) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // HIGH-LEVEL HEADER (Compact)
            Surface(tonalElevation = 2.dp) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Expenses", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { Firebase.auth.signOut() }) { Text("Sign Out") }
                    }

                    // SEARCH BAR (Takes up one full width line)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) } },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent)
                    )
                }
            }

            // FILTER ROW (Logic-based placement: Horizontal chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Filter Chip
                var catExpanded by remember { mutableStateOf(false) }
                val currentCat = categories.find { it.remoteId == selectedCategoryId }
                Box {
                    FilterChip(
                        selected = selectedCategoryId != null,
                        onClick = { catExpanded = true },
                        label = { Text(currentCat?.name ?: "Category") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                    )
                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        DropdownMenuItem(text = { Text("All Categories") }, onClick = { 
                            selectedCategoryId = null
                            selectedVendorId = null 
                            catExpanded = false 
                        })
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = { 
                                selectedCategoryId = c.remoteId
                                selectedVendorId = null // Clear vendor when category changes as requested
                                catExpanded = false 
                            })
                        }
                    }
                }

                // Vendor Filter Chip
                var venExpanded by remember { mutableStateOf(false) }
                val currentVen = vendors.find { it.remoteId == selectedVendorId }
                Box {
                    FilterChip(
                        selected = selectedVendorId != null,
                        onClick = { venExpanded = true },
                        label = { Text(currentVen?.name ?: "Vendor") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        enabled = true
                    )
                    DropdownMenu(expanded = venExpanded, onDismissRequest = { venExpanded = false }) {
                        DropdownMenuItem(text = { Text("All Vendors") }, onClick = { selectedVendorId = null; venExpanded = false })
                        vendorsForCategory.forEach { v ->
                            DropdownMenuItem(text = { Text(v.name) }, onClick = { selectedVendorId = v.remoteId; venExpanded = false })
                        }
                    }
                }
            }

            // THE LIST (Logical Results Area - Maximum Space)
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching expenses", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                ) {
                    items(filtered, key = { it.expense.remoteId }) { item ->
                        ExpenseCard(item, onEdit = { entryToEdit = item }, onDelete = { entryToDelete = item.expense })
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionsScreen(user: FirebaseUser) {
    val dbRef = Firebase.database.reference.child("users").child(user.uid).child("subscriptions")
    var subscriptions by remember { mutableStateOf(listOf<Subscription>()) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<Subscription?>(null) }
    var entryToDelete by remember { mutableStateOf<Subscription?>(null) }

    DisposableEffect(user.uid) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Subscription>()
                for (child in snapshot.children) {
                    val name = child.child("name").value?.toString() ?: ""
                    val account = child.child("account").value?.toString() ?: ""
                    val amount = child.child("amount").value?.toString() ?: ""
                    val dueDate = child.child("dueDate").value?.toString() ?: ""
                    val memo = child.child("memo").value?.toString() ?: ""
                    list.add(Subscription(id = child.key, name = name, account = account, amount = amount, dueDate = dueDate, memo = memo))
                }
                subscriptions = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        dbRef.addValueEventListener(listener)
        onDispose { dbRef.removeEventListener(listener) }
    }

    val filtered = subscriptions.filter { it.name.contains(searchQuery, ignoreCase = true) || it.account.contains(searchQuery, ignoreCase = true) }

    if (showAddDialog) {
        SubscriptionDialog(onDismiss = { showAddDialog = false }, onSave = { n, a, am, d, m ->
            dbRef.push().setValue(Subscription(name = n, account = a, amount = am, dueDate = d, memo = m))
            showAddDialog = false
        })
    }

    entryToEdit?.let { item ->
        SubscriptionDialog(initialName = item.name, initialAccount = item.account, initialAmount = item.amount, initialDueDate = item.dueDate, initialMemo = item.memo,
            onDismiss = { entryToEdit = null }, onSave = { n, a, am, d, m ->
                item.id?.let { dbRef.child(it).setValue(Subscription(name = n, account = a, amount = am, dueDate = d, memo = m)) }
                entryToEdit = null
            }
        )
    }

    entryToDelete?.let { item ->
        AlertDialog(onDismissRequest = { entryToDelete = null }, title = { Text("Delete Subscription?") }, text = { Text("Delete ${item.name}?") },
            confirmButton = { Button(onClick = { item.id?.let { dbRef.child(it).removeValue() }; entryToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) ) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { entryToDelete = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Color(0xFFFF9800), contentColor = Color.White, shape = CircleShape) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            TopHeader(user.email ?: "", "Subscriptions")
            SearchBar(searchQuery) { searchQuery = it }
            LazyColumn {
                items(filtered, key = { it.id ?: "" }) { item ->
                    SubscriptionCard(item, onEdit = { entryToEdit = item }, onDelete = { entryToDelete = item })
                }
            }
        }
    }
}

@Composable
fun TopHeader(email: String, title: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        TextButton(onClick = { Firebase.auth.signOut() }) { Text("Sign Out") }
    }
    Text(text = "Logged in as: $email", style = MaterialTheme.typography.bodySmall)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun SearchBar(query: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = query, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Clear, null) } },
        singleLine = true
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun PasswordCard(item: PwEntity, onCopy: () -> Unit, onView: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.vendor, style = MaterialTheme.typography.titleLarge)
                if (item.account.isNotBlank()) Text(text = item.account, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row {
                IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, null) }
                IconButton(onClick = onView) { Icon(Icons.Default.Visibility, null) }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun ExpenseCard(item: ExpenseWithDetails, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.vendorName ?: "Unknown Vendor", style = MaterialTheme.typography.titleLarge)
                Text(text = "${item.categoryName ?: "No Category"} • ${item.expense.amount}", style = MaterialTheme.typography.bodyMedium)
                Text(text = item.expense.date, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun SubscriptionCard(item: Subscription, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleLarge)
                Text(text = "${item.amount} • Due: ${item.dueDate}", style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun EntryDialog(title: String, initialVendor: String = "", initialAccount: String = "", initialPassword: String = "", onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var vendor by remember { mutableStateOf(initialVendor) }
    var account by remember { mutableStateOf(initialAccount) }
    var password by remember { mutableStateOf(initialPassword) }
    var originalPassword by remember { mutableStateOf(initialPassword) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = vendor, onValueChange = { vendor = it }, label = { Text("Vendor") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = account, onValueChange = { account = it }, label = { Text("Account") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            if (title.contains("Edit")) {
                OutlinedTextField(value = originalPassword, onValueChange = { originalPassword = it }, label = { Text("Original Password") }, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { clipboardManager.setText(AnnotatedString(originalPassword)); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.ContentCopy, null) } })
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Row {
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(password)); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.ContentCopy, null) }
                    IconButton(onClick = { password = generateStrongPassword() }) { Icon(Icons.Default.VpnKey, null) }
                } })
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { password = generateStrongPassword() }, modifier = Modifier.fillMaxWidth()) { Text("Generate Strong Password") }
        }
    }, confirmButton = { Button(onClick = { if (vendor.isNotBlank() && password.isNotBlank()) onSave(vendor, account, password) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDialog(
    initialVendorId: String? = null,
    initialCategoryId: String? = null,
    initialAmount: String = "",
    initialDate: String = "",
    initialMemo: String = "",
    categories: List<Category>,
    vendors: List<Vendor>,
    onDismiss: () -> Unit,
    onSave: (String?, String?, String, String, String) -> Unit,
    onAddCategory: (String) -> Unit,
    onAddVendor: (String) -> Unit
) {
    var selectedVendorId by remember { mutableStateOf(initialVendorId) }
    var selectedCategoryId by remember { mutableStateOf(initialCategoryId) }
    var amount by remember { mutableStateOf(initialAmount) }
    var memo by remember { mutableStateOf(initialMemo) }
    
    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val initialDateMillis = remember(initialDate) {
        if (initialDate.isNotEmpty()) {
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                sdf.parse(initialDate)?.time
            } catch (e: Exception) { null }
        } else null
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis ?: System.currentTimeMillis())
    val formattedDate = remember(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.format(java.util.Date(it))
        } ?: initialDate
    }

    var catExpanded by remember { mutableStateOf(false) }
    var venExpanded by remember { mutableStateOf(false) }
    var showAddCatDialog by remember { mutableStateOf(false) }
    var showAddVenDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    if (showAddCatDialog || showAddVenDialog) {
        AlertDialog(
            onDismissRequest = { showAddCatDialog = false; showAddVenDialog = false },
            title = { Text(if (showAddCatDialog) "Add Category" else "Add Vendor") },
            text = { OutlinedTextField(newName, { newName = it }, label = { Text("Name") }) },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        if (showAddCatDialog) onAddCategory(newName) else onAddVendor(newName)
                        newName = ""; showAddCatDialog = false; showAddVenDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddCatDialog = false; showAddVenDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Expense Details") }, text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // Vendor Dropdown
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                OutlinedButton(onClick = { venExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(vendors.find { it.remoteId == selectedVendorId }?.name ?: "Select Vendor")
                }
                DropdownMenu(expanded = venExpanded, onDismissRequest = { venExpanded = false }) {
                    vendors.forEach { v ->
                        DropdownMenuItem(text = { Text(v.name) }, onClick = { selectedVendorId = v.remoteId; venExpanded = false })
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("+ Add New Vendor", color = MaterialTheme.colorScheme.primary) }, 
                        onClick = { venExpanded = false; showAddVenDialog = true })
                }
            }

            // Category Dropdown
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                OutlinedButton(onClick = { catExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(categories.find { it.remoteId == selectedCategoryId }?.name ?: "Select Category")
                }
                DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    categories.forEach { c ->
                        DropdownMenuItem(text = { Text(c.name) }, onClick = { selectedCategoryId = c.remoteId; catExpanded = false })
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("+ Add New Category", color = MaterialTheme.colorScheme.primary) }, 
                        onClick = { catExpanded = false; showAddCatDialog = true })
                }
            }

            OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
            
            // Date Picker Trigger
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Date: $formattedDate")
            }

            OutlinedTextField(memo, { memo = it }, label = { Text("Memo") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = {
        Button(onClick = { onSave(selectedVendorId, selectedCategoryId, amount, formattedDate, memo) }) { Text("Save") }
    }, dismissButton = {
        TextButton(onClick = onDismiss) { Text("Cancel") }
    })
}

@Composable
fun SubscriptionDialog(initialName: String = "", initialAccount: String = "", initialAmount: String = "", initialDueDate: String = "", initialMemo: String = "", onDismiss: () -> Unit, onSave: (String, String, String, String, String) -> Unit) {
    var n by remember { mutableStateOf(initialName) }; var a by remember { mutableStateOf(initialAccount) }; var am by remember { mutableStateOf(initialAmount) }; var d by remember { mutableStateOf(initialDueDate) }; var m by remember { mutableStateOf(initialMemo) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Subscription Details") }, text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(n, {n=it}, label={Text("Name")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(a, {a=it}, label={Text("Account")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(am, {am=it}, label={Text("Amount")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(d, {d=it}, label={Text("Due Date")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(m, {m=it}, label={Text("Memo")}, modifier=Modifier.fillMaxWidth())
        }
    }, confirmButton = { Button(onClick = { onSave(n,a,am,d,m) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun ViewPasswordDialog(entity: PwEntity, onDismiss: () -> Unit, onCopy: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(entity.vendor) }, text = {
        Column {
            if (entity.account.isNotBlank()) Text("Account: ${entity.account}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Password:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(entity.pw, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }, confirmButton = { Button(onClick = onCopy) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Copy") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

fun generateStrongPassword(length: Int = 12): String {
    val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
    return (1..length).map { charPool.random() }.joinToString("")
}
