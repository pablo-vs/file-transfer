[//]: # (Pablo Villalobos Sánchez)

# Servidor y cliente de transferencia de ficheros

## Parte 2 - Modelo híbrido p2p

```
################################
##### File Transfer Client #####
################################

ID usuario: user1
Servidor: localhost/127.0.0.1
Puerto: 5555
Localhost: HomePC/10.0.0.15
Estado: Conectado

################################
Conectando...
Conectado
Cargando lista de usuarios...
Lista cargada
################################

Comandos básicos:
c: Conectar      p: Pedir fich. 
s: Salir         a: Ayuda       
<Enter>: Actualizar             

Introduzca un comando:
```


Esta práctica está escrita para funcionar en Linux, pero también
funciona en Windows. No funciona con java 8, pero sí con java 13
y 14.

## Compilación

### Linux

Se puede compilar con `make`, los archivos .class aparecerán en `build`.  
`make clean` elimina los archivos compilados.

### Windows

Desde la raíz del proyecto, ejecute
```
dir /s /b *.java > sources.txt
javac @sources.txt -d build
```

## Ejecución

Dentro del directorio de compilación, el servidor se puede ejecutar con
el siguiente comando:

```
java server.Servidor <puerto>
```

El mensaje "Iniciando servidor..." debería aparecer por pantalla.  

El cliente se puede ejecutar mediante:
```
java client.CotroladorCliente
```

El cliente pedirá al inicio la dirección y puerto del servidor, así
como el identificador del usuario y el directorio de archivos compartidos.
Estas opciones también se pueden especificar como argumentos de
línea de comandos
```
java client.ControladorCliente localhost 5555 'user1' '../ficheros'.
```

Una vez introducidos todos los datos, entrará en el menú principal de la
aplicación, desde el cual puede ejecutar comandos escribiendo la letra
del comando y pulsando <kbd>Enter</kbd>.  

Añadiendo la opción `-Djava.util.logging.config.file="../logging.properties"`
se puede modificar la configuración de logging, en particular el log level.
Los niveles INFO, FINE y FINER revelan progresivamente más información, tanto
en el cliente como en el servidor.
