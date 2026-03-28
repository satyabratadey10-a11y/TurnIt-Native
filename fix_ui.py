import os

path = "app/src/main/java/com/turnit/app/ui/Composables.kt"
with open(path, "r") as f:
    content = f.read()

# Fix 1: Ensure @OptIn is on the main screen function
if "@OptIn(ExperimentalMaterial3Api::class)" not in content:
    content = content.replace("fun TurnItMainScreen", "@OptIn(ExperimentalMaterial3Api::class)\nfun TurnItMainScreen")

# Fix 2: Explicitly align TopAppBar color parameters
content = content.replace("containerColor = Color(0xCC0B0E14)", "containerColor = Color(0xCC0B0E14)")
content = content.replace("titleContentColor = QX.TextPrimary", "titleContentColor = QX.TextPrimary")
content = content.replace("navigationIconColor = QX.QuantumTeal", "navigationIconContentColor = QX.QuantumTeal")

with open(path, "w") as f:
    f.write(content)
