const express = require('express');
const mysql = require('mysql2');

const app = express();
const port = process.env.PORT || 443;

// Conexão MySQL
const db = mysql.createConnection({
  host: '',
  user: '', 
  password: '', 
  database: '',
  ssl:{
    rejectUnauthorized: false
}
});

db.connect((err) => {
  if (err) {
    console.error('Erro ao conectar a database', err);
    return;
  }
  console.log('Conectado a Database MySQL');
});

app.use(express.json()); // Json para POST

// POST para adicionar usuario
app.post("/usuario", function (req, res) {
  console.log(req.body);
  var nome = req.body.nome;
  var senha = req.body.senha;
  var email = req.body.email;
  var celular = req.body.celular;
  console.log(nome + " " + senha + " " + email + " " + celular);
  var query = 'INSERT INTO USUARIO (nome, senha, email, telefone) VALUES (?, ?, ?, ?)';
  db.query(query, [nome, senha, email, celular], (err, result) => {
    if (err) {
      console.error('Falha ao adicionar usuario', err);
      return res.status(500).json({ error: 'Falha ao adicionar usuario' + err.message });
    }
    res.status(201).json({ message: 'Usuario adicionado' });
  });
});

// GET para buscar todos os usuarios(Teste)
app.get("/tudo", function (req, res) {
  console.log("GET /tudo route was hit");
  db.query('SELECT * FROM usuario', (err, rows) => {
    if (err) {
      console.error('Erro buscando usuarios:', err);
      return res.status(500).json({ error: 'Erro ao buscar usuarios' });
    }
    console.log('Usuarios:', rows);
    res.json(rows); 
  });
});

app.post("/login", function(req, res) {
  var email = req.body.email;
  var senha = req.body.senha;
  var query1 = 'SELECT * FROM usuario WHERE email = ?';
  
  db.query(query1, [email], (err, rows) => {
      if (err) {
          console.error('Erro buscando usuarios:', err);
          return res.status(500).json({ error: 'Erro buscando usuarios' });
      }
      
      if (rows.length === 0) {
          return res.status(404).json({ error: 'Usuario não encontrado' });
      }

      var userSenha = rows[0].senha;
      
      //Valida a senha
      if (userSenha !== senha) {
          return res.status(404).json({ error: 'Senha incorreta' });
      }

      // Retorna somente nome, email e telefone
      var userData = {
          nome: rows[0].nome,
          email: rows[0].email,
          telefone: rows[0].telefone
      };

      res.json(userData);  //Manda os dados como Json
  });
});


app.get("/", function (req, res) {
  res.send("Server is running!");
});


// Inicializar o Servidor
app.listen(port, () => {
  console.log(`Server running on https://uberproject-cjajhtamdkemhqd2.canadacentral-01.azurewebsites.net:${port}`);
});
