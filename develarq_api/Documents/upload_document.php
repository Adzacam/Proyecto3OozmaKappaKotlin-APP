<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Manejo de solicitud OPTIONS (Pre-flight para CORS)
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../db_config/database.php';

// ==========================================
// 1. MANEJO DE TOKEN (PARCHE XAMPP)
// ==========================================
$headers = getallheaders();

if (!isset($headers['Authorization']) && isset($_SERVER['HTTP_AUTHORIZATION'])) {
    $headers['Authorization'] = $_SERVER['HTTP_AUTHORIZATION'];
}

if (!isset($headers['Authorization'])) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$authHeader = $headers['Authorization'];
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
    $token = $matches[1];
} else {
    $token = $authHeader;
}

$database = new Database();
$db = $database->getConnection();

// ==========================================
// 2. OBTENER USER ID REAL
// ==========================================
$user_id = null; 

try {
    // Buscamos al usuario dueño del token
    $query = "SELECT id FROM users WHERE remember_token = :token AND eliminado = 0 LIMIT 1";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":token", $token);
    $stmt->execute();

    if ($stmt->rowCount() > 0) {
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        $user_id = $row['id'];
    //} else {
       // http_response_code(401);
        //echo json_encode([
            //"success" => false, 
           // "message" => "TOKEN NO COINCIDE. Recibido: [" . $token . "]"
        //]);
       // exit();
   // }
   } else {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Autenticación fallida: Token inválido o usuario no encontrado."]);
        exit();
    }
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Error al validar usuario: " . $e->getMessage()]);
    exit();
}
// ==========================================
// 3. PROCESAR SUBIDA
// ==========================================
try {
    if (!isset($_POST['proyecto_id']) || !isset($_POST['nombre']) || !isset($_POST['tipo'])) {
        throw new Exception("Datos incompletos (proyecto_id, nombre, tipo son requeridos)");
    }

    $proyecto_id = $_POST['proyecto_id'];
    $nombre = $_POST['nombre'];
    $descripcion = isset($_POST['descripcion']) ? $_POST['descripcion'] : null;
    $tipo = $_POST['tipo'];
    $enlace_externo = isset($_POST['enlace_externo']) ? $_POST['enlace_externo'] : null;
    
    $archivo_url = "";

    if ($tipo === 'URL') {
        if (empty($enlace_externo)) {
            throw new Exception("El enlace externo es requerido para tipo URL");
        }
        $archivo_url = $enlace_externo; 
    } else {
     
        if (!isset($_FILES['archivo']) || $_FILES['archivo']['error'] !== UPLOAD_ERR_OK) {
             throw new Exception("No se ha enviado el archivo o hubo un error en la subida");
        }
        $uploadDir = 'C:/Users/HP/Documents/Univalle/6to Semestre proyecto Develarq/Proyecto3OozmaKappa/storage/app/public/documentos/';
        if (!file_exists($uploadDir)) {
            mkdir($uploadDir, 0777, true);
        }

        $fileName = uniqid() . '_' . basename($_FILES['archivo']['name']);
        $targetPath = $uploadDir . $fileName;

        if (move_uploaded_file($_FILES['archivo']['tmp_name'], $targetPath)) {
        
            $archivo_url = 'documentos/' . $fileName;
} else {
            throw new Exception("Error al mover el archivo al servidor");
        }
    }

    $query = "INSERT INTO documentos (proyecto_id, nombre, descripcion, tipo, archivo_url, enlace_externo, subido_por, fecha_subida, created_at, updated_at) 
          VALUES (:proyecto_id, :nombre, :descripcion, :tipo, :archivo_url, :enlace_externo, :subido_por, NOW(), NOW(), NOW())";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":proyecto_id", $proyecto_id);
    $stmt->bindParam(":nombre", $nombre);
    $stmt->bindParam(":descripcion", $descripcion);
    $stmt->bindParam(":tipo", $tipo);
    $stmt->bindParam(":archivo_url", $archivo_url);
    $stmt->bindParam(":enlace_externo", $enlace_externo);
    $stmt->bindParam(":subido_por", $user_id);

    if ($stmt->execute()) {
       
        $lastId = $db->lastInsertId();
        
        $stmtSelect = $db->prepare("SELECT * FROM documentos WHERE id = ?");
        $stmtSelect->execute([$lastId]);
        $newDoc = $stmtSelect->fetch(PDO::FETCH_ASSOC);

        http_response_code(200);
        echo json_encode([
            "success" => true, 
            "message" => "Documento subido correctamente",
            "data" => $newDoc
        ]);
    } else {
        throw new Exception("Error al guardar en la base de datos");
    }

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false, 
        "message" => $e->getMessage()
    ]);
}
?>