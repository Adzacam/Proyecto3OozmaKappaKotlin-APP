<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../db_config/database.php';
include_once '../db_config/audit_helper.php';

$database = new Database();
$db = $database->getConnection();

// AUTENTICACIÓN
$headers = getallheaders();
$token = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;

if (!$token) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$usuario = obtenerUsuarioDesdeToken($db, $token);
if (!$usuario) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token inválido"]);
    exit();
}

// VALIDAR PERMISOS
$rol_permitido = in_array(strtolower($usuario['rol']), ['admin', 'ingeniero', 'arquitecto']);
if (!$rol_permitido) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "No tienes permisos para subir planos"]);
    exit();
}

// RECIBIR DATOS
$proyecto_id = isset($_POST['proyecto_id']) ? intval($_POST['proyecto_id']) : null;
$nombre = isset($_POST['nombre']) ? trim($_POST['nombre']) : null;
$descripcion = isset($_POST['descripcion']) ? trim($_POST['descripcion']) : null;
$tipo = isset($_POST['tipo']) ? trim($_POST['tipo']) : null;
$enlace_externo = isset($_POST['enlace_externo']) ? trim($_POST['enlace_externo']) : null;

// Información del dispositivo
$device_model = isset($_POST['device_model']) ? $_POST['device_model'] : 'No disponible';
$android_version = isset($_POST['android_version']) ? $_POST['android_version'] : 'No disponible';
$sdk_version = isset($_POST['sdk_version']) ? $_POST['sdk_version'] : 'No disponible';

// VALIDACIONES
if (!$proyecto_id || !$nombre || !$tipo) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Faltan datos obligatorios"]);
    exit();
}

// ============================================
// ✅ INICIALIZAR VERSIÓN ANTES DEL TRY
// ============================================
$nueva_version = '1.0';
$es_nueva_version = false;

try {
    // ============================================
    // CALCULAR VERSIÓN AUTOMÁTICAMENTE
    // ============================================
    $query_version = "SELECT MAX(CAST(SUBSTRING_INDEX(COALESCE(version, '1.0'), '.', -1) AS UNSIGNED)) as max_minor
                      FROM planos_bim 
                      WHERE nombre = :nombre 
                      AND proyecto_id = :proyecto_id 
                      AND eliminado = 0";

    $stmt_version = $db->prepare($query_version);
    $stmt_version->execute([
        ':nombre' => $nombre,
        ':proyecto_id' => $proyecto_id
    ]);

    $version_actual = $stmt_version->fetch(PDO::FETCH_ASSOC);

    if ($version_actual && $version_actual['max_minor'] !== null) {
        $minor = intval($version_actual['max_minor']) + 1;
        $nueva_version = "1.$minor";
        $es_nueva_version = true;
    }

} catch (Exception $e) {
    error_log("Error al calcular versión: " . $e->getMessage());
    // Continuar con versión 1.0
}

// ============================================
// SUBIR ARCHIVO SI EXISTE
// ============================================
$archivo_url = null;

if (isset($_FILES['archivo']) && $_FILES['archivo']['error'] == 0) {
    $directorio_base = "C:/Users/HP/Documents/Univalle/6to Semestre proyecto Develarq/Proyecto3OozmaKappa/storage/app/public/planos/proyecto_$proyecto_id/";
    
    // ✅ CREAR DIRECTORIO CON PERMISOS
    if (!file_exists($directorio_base)) {
        if (!mkdir($directorio_base, 0777, true)) {
            http_response_code(500);
            echo json_encode(["success" => false, "message" => "Error al crear directorio"]);
            exit();
        }
        chmod($directorio_base, 0777);
    }
    
    // ✅ DETECTAR EXTENSIÓN CORRECTAMENTE
    $extension = strtolower(pathinfo($_FILES['archivo']['name'], PATHINFO_EXTENSION));
    
    // Si no hay extensión, usar el tipo MIME
    if (empty($extension)) {
        $mime_type = $_FILES['archivo']['type'];
        $extension_map = [
            'model/gltf-binary' => 'glb',
            'application/octet-stream' => 'fbx',
            'image/jpeg' => 'jpg',
            'image/png' => 'png',
            'application/pdf' => 'pdf'
        ];
        $extension = $extension_map[$mime_type] ?? 'bin';
    }
    
    $nombre_archivo = "plano_" . uniqid() . "." . $extension;
    $ruta_completa = $directorio_base . $nombre_archivo;
    
    // ✅ INTENTAR SUBIR CON MEJOR MANEJO DE ERRORES
    if (move_uploaded_file($_FILES['archivo']['tmp_name'], $ruta_completa)) {
        chmod($ruta_completa, 0666);
        $archivo_url = "/storage/planos/proyecto_$proyecto_id/$nombre_archivo";
    } else {
        $error_upload = error_get_last();
        http_response_code(500);
        echo json_encode([
            "success" => false, 
            "message" => "Error al subir archivo: " . ($error_upload['message'] ?? "Desconocido"),
            "debug" => [
                "directorio" => $directorio_base,
                "existe" => file_exists($directorio_base),
                "permisos" => substr(sprintf('%o', fileperms($directorio_base)), -4),
                "tmp_file" => $_FILES['archivo']['tmp_name'],
                "destino" => $ruta_completa
            ]
        ]);
        exit();
    }
} elseif ($enlace_externo) {
    $archivo_url = $enlace_externo;
} else {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Debe proporcionar un archivo o un enlace externo"]);
    exit();
}

// ============================================
// INSERTAR EN BASE DE DATOS
// ============================================
try {
    $query = "INSERT INTO planos_bim 
              (proyecto_id, nombre, descripcion, tipo, archivo_url, version, subido_por, created_at, updated_at) 
              VALUES 
              (:proyecto_id, :nombre, :descripcion, :tipo, :archivo_url, :version, :subido_por, NOW(), NOW())";
    
    $stmt = $db->prepare($query);
    $stmt->execute([
        ':proyecto_id' => $proyecto_id,
        ':nombre' => $nombre,
        ':descripcion' => $descripcion,
        ':tipo' => $tipo,
        ':archivo_url' => $archivo_url,
        ':version' => $nueva_version,
        ':subido_por' => $usuario['id']
    ]);
    
    $plano_id = $db->lastInsertId();
    
    // AUDITORÍA
    $device_info = [
        'device_model' => $device_model,
        'android_version' => $android_version,
        'sdk_version' => $sdk_version
    ];
    
    $accion_mensaje = $es_nueva_version 
        ? "Subió la versión $nueva_version del plano BIM '$nombre' al proyecto ID $proyecto_id"
        : "Subió el plano BIM '$nombre' (v$nueva_version) al proyecto ID $proyecto_id";

    registrarAuditoriaCompleta(
        $db,
        $usuario['id'],
        $accion_mensaje,
        'planos_bim',
        $plano_id,
        $device_info
    );
    
    echo json_encode([
        "success" => true,
        "message" => "Plano subido correctamente",
        "data" => [
            "id" => intval($plano_id),
            "nombre" => $nombre,
            "archivo_url" => $archivo_url,
            "version" => $nueva_version
        ]
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al guardar plano: " . $e->getMessage()
    ]);
}
?>