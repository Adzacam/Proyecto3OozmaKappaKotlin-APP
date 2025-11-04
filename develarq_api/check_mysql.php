<?php
echo "Verificando MySQL...<br>";

// Probar diferentes configuraciones
$configs = [
    ["host" => "localhost", "port" => 3306],
    ["host" => "127.0.0.1", "port" => 3306],
    ["host" => "localhost", "port" => 3307],
    ["host" => "127.0.0.1", "port" => 3307]
];

foreach ($configs as $config) {
    $host = $config["host"];
    $port = $config["port"];
    
    echo "Probando $host:$port... ";
    
    // Probar conexión básica
    $socket = @fsockopen($host, $port, $errno, $errstr, 2);
    if ($socket) {
        echo "✅ CONEXIÓN EXITOSA<br>";
        fclose($socket);
        
        // Probar con PDO
        try {
            $pdo = new PDO("mysql:host=$host;port=$port", "root", "");
            echo "✅ PDO también funciona<br>";
        } catch(PDOException $e) {
            echo "❌ PDO falló: " . $e->getMessage() . "<br>";
        }
    } else {
        echo "❌ Falló: $errstr<br>";
    }
}
?>