📂 DevelArq API - Guía de Configuración
Este documento detalla los pasos necesarios para desplegar la API de backend y configurar la aplicación móvil para el entorno de desarrollo local.

📋 Prerrequisitos
XAMPP (o similar con Apache y MySQL).

Composer instalado globalmente.

Android Studio (para la app móvil).

🚀 1. Instalación del Servidor (Backend)
Ubicación del Proyecto
Para asegurar el correcto funcionamiento con XAMPP, mueve o clona la carpeta del proyecto en la siguiente ruta:

\xampp\htdocs\develarq_api
Instalación de Dependencias
El sistema utiliza PHPMailer para el envío de correos electrónicos. Debes instalar las dependencias mediante Composer.

Abre una terminal (CMD o PowerShell).

Navega a la raíz del proyecto:

Bash

cd c:\xampp\htdocs\develarq_api\
Ejecuta el siguiente comando:

Bash

composer require phpmailer/phpmailer
⚙️ 2. Configuración del Backend
Debes editar varios archivos para que coincidan con tu entorno local y rutas de archivos.

A. Base de Datos
Edita el archivo config/database.php (o la ruta donde esté tu conexión). Verifica que el puerto coincida con la configuración de tu MySQL en XAMPP (por defecto suele ser 3306, pero a veces es 3307 o 3308).

PHP

// En database.php
private $port = "3306"; // Cambia esto según tu puerto
B. URL de Restablecimiento de Contraseña
Para que los enlaces de recuperación de contraseña funcionen en los correos enviados, edita el archivo:

📂 forgot-password/index.php

PHP

// Reemplaza TU_IP con la IP de tu máquina (ej: 192.168.1.X)
$resetLink = "http://TU_IP/develarq_api/reset-password/index.php?token=" . $token;
C. Rutas de Almacenamiento (Documentos)
Debes configurar la ruta absoluta donde se guardarán los archivos.

📂 Archivo: documents.php (o el controlador encargado de subir documentos).

Busca la variable $uploadDir y actualízala con la ruta de TU ordenador:

PHP

// Ejemplo: Cambia la parte 'C:/Users/HP/...' por tu usuario y ruta real
$uploadDir = 'C:/Users/TU_USUARIO/Documents/.../Proyecto3OozmaKappa/storage/app/public/documentos/';
D. Rutas de Almacenamiento (Planos BIM)
Similar al paso anterior, configura la ruta para los planos BIM.

📂 Archivos: upload_bim_plano.php y otros relacionados con BIM.

PHP

// Actualiza la variable $directorio_base con tu ruta local
$directorio_base = "C:/Users/TU_USUARIO/Documents/.../Proyecto3OozmaKappa/storage/app/public/planos/proyecto_$proyecto_id/";
🔐 3. Permisos de Escritura (Windows)
Para que PHP pueda subir y sobrescribir archivos en las carpetas de almacenamiento (especialmente para planos BIM), necesitas otorgar permisos de escritura en Windows.

Abre el CMD como Administrador.

Navega a la carpeta pública de almacenamiento (ajusta la ruta a la tuya):

DOS

cd "C:\Users\HP\Documents\Univalle\6to Semestre proyecto Develarq\Proyecto3OozmaKappa\storage\app\public"
Ejecuta el comando icacls para dar permisos totales:

DOS

icacls planos /grant Todos:(OI)(CI)F /T
(Esto otorga control total a "Todos" los usuarios sobre la carpeta "planos" y sus subcarpetas).

📱 4. Configuración del Cliente (Android App)
Para que la aplicación móvil se conecte a tu API local, debes especificar la dirección IP de tu servidor (tu computadora).

Abre el proyecto en Android Studio.

Ubica el archivo de configuración. Generalmente se encuentra en uno de estos dos lugares:

📂 build.gradle (nivel app)

📂 local.properties (Recomendado para no subir IPs locales a Git)

Cambia la BASE_URL o IP por la IP de tu máquina (ej. 192.168.1.5).
