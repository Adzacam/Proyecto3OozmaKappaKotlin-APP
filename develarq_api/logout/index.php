<?php
// Incluir los encabezados para CORS
include_once '../db_config/database.php';

// Simplemente confirmamos que la petición de logout fue recibida.
// La app de Kotlin debe encargarse de borrar el token localmente.
http_response_code(200);
echo json_encode(["success" => true, "message" => "Logout exitoso"]);
?>