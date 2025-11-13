<?php
class Database {
    private $host = "127.0.0.1";
    private $port = "3308";
    private $db_name = "develarq";
    private $username = "root";
    private $password = "";
    public $conn;

    public function getConnection() {
        $this->conn = null;
        try {
            $dsn = "mysql:host={$this->host};port={$this->port};dbname={$this->db_name};charset=utf8";
            $this->conn = new PDO($dsn, $this->username, $this->password);
            $this->conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            // Puedes imprimir esto para confirmar conexión
            // echo json_encode(["success" => true, "message" => "Conexión exitosa"]);
        } catch (PDOException $exception) {
            echo json_encode([
                "success" => false,
                "message" => "Error de conexión: " . $exception->getMessage()
            ]);
            exit();
        }
        return $this->conn;
        
    }
    
}
?>
