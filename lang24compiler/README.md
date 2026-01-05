# Gradlified Prevajalnik

## Kako to dela?
1. Namesti plugin, ki ga predlaga VSCode za Gradle (od Microsofta)
2. Na levi strani se ti pojavi ikonica slončka, tam so gradle akcije
   1. Zanima te tasks->application->run (lahko klikneš na hrošča in bo debug)

## Težave
Meni se včasih v Intellij IDEA zatakne, ker naredi znotraj mape src/ še mapo
gen, kjer zgenerira kodo ... in je treba ročno zbrisat. Morda je VSCode bolj pameten?

## Ročno poganjanje
Poleg make-a lahko poganjaš tudi z ukazom
```bash
./gradlew run -PsrcFileName=ime_testa
```
kjer je ime_testa ime datoteke **brez** končnice `.lang24`.