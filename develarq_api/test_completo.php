<?php
echo "=== INFORMACIÓN COMPLETA ===<br>";
echo "PHP Version: " . phpversion() . "<br>";
echo "php.ini: " . php_ini_loaded_file() . "<br>";
echo "Archivo actual: " . __FILE__ . "<br><br>";

echo "=== EXTENSIONES DATABASE ===<br>";
echo "PDO: " . (extension_loaded('pdo') ? '✅' : '❌') . "<br>";
echo "PDO MySQL: " . (extension_loaded('pdo_mysql') ? '✅' : '❌') . "<br>";
echo "MySQLi: " . (extension_loaded('mysqli') ? '✅' : '❌') . "<br><br>";

echo "=== PRUEBA CONEXIÓN ===<br>";
try {
    $db = new PDO("mysql:host=127.0.0.1;port=3306;dbname=develarq", "root", "");
    echo "✅ Conexión MySQL exitosa<br>";
    
    // Probar consulta simple
    $stmt = $db->query("SELECT 1 as test");
    $result = $stmt->fetch();
    echo "✅ Consulta SQL funcionando<br>";
    
} catch(PDOException $e) {
    echo "❌ Error conexión: " . $e->getMessage() . "<br>";
}

echo "<br>=== ¿ARCHIVO PHP O TEXTO? ===";
echo "<br>Si ves esta línea con formato, PHP está ejecutándose correctamente";
?>