import zipfile,io,re
z=zipfile.ZipFile("GoetyRevelation-2.3.3fix.jar")
for n in z.namelist():
 if n.endswith(".jar"):
  zz=zipfile.ZipFile(io.BytesIO(z.read(n))); print("---",n)
  for x in zz.namelist():
   if x.endswith(".class") and any(k in x.lower() for k in ["crystal","youkai","ring","gun","spear","end","inferno"]): print(x)
