
1. el archivo develarq_api tiene que estar en la siguiente direcion : \xampp\htdocs\develarq_api
Ejecutar en la raíz de tu proyecto digamos (c:\xampp\htdocs\develarq_api/):

2. Dependencias de PHP y Composer
El envío de correos requiere la biblioteca PHPMailer.
Ejecutar composer require phpmailer/phpmailer

3. en el database.php
private $port = ""; ver que puerto usas

4. URL de Restablecimiento
Asegúrate de que la URL de restablecimiento en forgot-password/index.php apunte a tu entorno local:
// En forgot-password/index.php
$resetLink = "http://TU_IP/develarq_api/reset-password/index.php?token=" . $token;
