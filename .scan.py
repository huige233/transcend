import zipfile,glob
for j in glob.glob("*.jar"):
 z=zipfile.ZipFile(j); print("---",j)
 for n in z.namelist():
  if n.endswith(".class") and any(x in n.lower() for x in ["inferno","crystal","youkai","ring","eloro","spear","fairy","return","end"]): print(n)
