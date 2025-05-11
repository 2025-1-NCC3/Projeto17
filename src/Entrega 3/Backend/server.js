const express = require('express');
const mysql = require('mysql2');

const app = express();
const port = process.env.PORT || 443;

// Conexão MySQL
const db = mysql.createConnection({
  host: process.env.host,
  user: process.env.user, 
  password: process.env.password, 
  database: process.env.database,
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
  var senhaDescript = Descriptografar(senha, email);
  var userSenhaDescript;

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
      userSenhaDescript = Descriptografar(userSenha, email);
      
      //Valida a senha
      if (userSenhaDescript !== senhaDescript) {
          return res.status(404).json({ error: 'Senha incorreta' });
      }

      // Retorna somente nome, email e telefone
      var userData = {
          nome: rows[0].nome,
          email: rows[0].email,
          celular: rows[0].telefone
      };

      res.json(userData);  //Manda os dados como Json
  });
});
// Post para deletar o usuario
app.post("/deletar", function(req, res){
  var email = req.body.email;
  var query1 = 'Select * from usuario where email = ?';
  var query2 = 'delete from alerta where idUsuario = ?'
  var query3 = 'delete from usuario where id = ?'

  db.query(query1, [email], (err, rows)=>{
    if (err) {
      console.error('Erro buscando usuarios:', err);
      return res.status(500).json({ error: 'Erro buscando usuarios:' + err });
  }
  
  if (rows.length === 0) {
      return res.status(404).json({ error: 'Usuario não encontrado' });
  }
  var id = rows[0].id;
  db.query(query2, [id],(err, rows) =>{
    if (err) {
      console.error('Erro buscando usuarios:', err);
      return res.status(500).json({ error: 'Erro buscando usuarios' + err });
  }
  })
  db.query(query3, [id], (err, rows) =>{
    if (err) {
      console.error('Erro buscando usuarios:', err);
      return res.status(500).json({ error: 'Erro buscando usuarios' + err });
  }
  return res.status(200).json({ message: "Usuario deletado com sucesso!" });
  })
  })
})

app.post("/emitirAlerta", function(req, res){
  var idUser;
  var nomeUsuario = req.body.nomeUsuario;
  var tipo = req.body.tipoAlerta;
  var latitude = req.body.latitude;
  var longitude = req.body.longitude;
  var query = "Insert into alerta (tipoAlerta, latitude, longitude, idUsuario) values (?, ?, ?, ?)";
  var query2 = "Select id from usuario where nome = ?"

  db.query(query2, [nomeUsuario],(err, rows) =>{
    if (err) {
      console.error('Usuario não encontrado', err);
      return res.status(500).json({ error: 'Usuario não encontrado' + err.message });
    }
    if (rows.length === 0) {
      return res.status(404).json({ error: 'Usuario não encontrado' });
  }
    idUser = rows[0].id;
    db.query(query, [tipo, latitude, longitude, idUser], (err, rows) =>{
      if (err) {
        console.error('Falha ao adicionar alerta', err);
        return res.status(500).json({ error: 'Falha ao adicionar alerta' + err.message });
      }
      res.status(201).json({ message: 'Alerta adicionado!' });
    })
  })

  
})

app.get("/buscarAlerta", function(req, res){
  var query = "Select * from alerta";

  db.query(query, (err, rows) =>{
    if (err) {
      console.error('Falha ao buscar alertas', err);
      return res.status(500).json({ error: 'Falha ao buscar alertas' + err.message });
    }
    console.log('Alertas:', rows);
    res.json(rows); 
  })
})

//Updates
app.post("/mudarNome", function(req, res){
  var novoNome = req.body.nome;
  var senhaCripto = req.body.senha;
  var email = req.body.email;
  var senhaDescript = Descriptografar(senhaCripto, email);
  var query1 = "Select * from usuario where email = ?";
  var query2 = "Update usuario set nome = ? where email = ?";
  db.query(query1, [email], (err, rows)=>{
    var senhaUsuario = Descriptografar(rows[0].senha, email);
    if(senhaDescript != senhaUsuario){
      return res.status(404).json({ error: 'Senha incorreta' });
    }
    db.query(query2,[novoNome,email],(err, rows)=>{
      if (err) {
        console.error('Falha ao modificar nome', err);
        return res.status(500).json({ error: 'Falha ao modificar nome' + err.message });
      }
      res.status(201).json({ message: 'Nome modificado!' });
    })
  })
})

app.post("/mudarTelefone", function(req, res){
  var novoTelefone = req.body.celular;
  var senhaCripto = req.body.senha;
  var email = req.body.email;
  var senhaDescript = Descriptografar(senhaCripto, email);
  var query1 = "Select * from usuario where email = ?";
  var query2 = "Update usuario set telefone = ? where email = ?";
  db.query(query1, [email], (err, rows)=>{
    var senhaUsuario = Descriptografar(rows[0].senha, email);
    if(senhaDescript != senhaUsuario){
      return res.status(404).json({ error: 'Senha incorreta' });
    }
    db.query(query2,[novoTelefone, email],(err, rows)=>{
      if (err) {
        console.error('Falha ao modificar telefone', err);
        return res.status(500).json({ error: 'Falha ao modificar telefone' + err.message });
      }
      res.status(201).json({ message: 'Telefone modificado!' });
    })
  })
})

app.post("/mudarEmail", function(req, res){
  var novoEmail = req.body.nome;
  var senhaCripto = req.body.senha;
  var antigoEmail = req.body.email;
  var senhaDescript = Descriptografar(senhaCripto, antigoEmail);
  var query1 = "Select * from usuario where email = ?";
  var query2 = "Update usuario set email = ?, senha = ?, telefone = ?, nome = ? where email = ?";
  db.query(query1, [antigoEmail], (err, rows)=>{
    var senhaUsuario = Descriptografar(rows[0].senha, antigoEmail);
    var nomeUsuario = Descriptografar(rows[0].nome, antigoEmail);
    var telefoneUsuario = Descriptografar(rows[0].telefone, antigoEmail);
    if(senhaDescript != senhaUsuario){
      return res.status(404).json({ error: 'Senha incorreta' });
    }
    senhaCripto = Criptografar(senhaUsuario, novoEmail);
    var nomeCripto = Criptografar(nomeUsuario,novoEmail);
    var telefoneCripto = Criptografar(telefoneUsuario, novoEmail);
    db.query(query2, [novoEmail, senhaCripto, telefoneCripto, nomeCripto, antigoEmail],(err, rows)=>{
      if (err) {
        console.error('Falha ao modificar email', err);
        return res.status(500).json({ error: 'Falha ao modificar email' + err.message });
      }
      res.status(201).json({ message: 'Email modificado!' });
    })
  })
})

//Entrada para log de erros do app(Para testes)
app.post("/logErro", function(req, res){
  var logErro = req.body.logErro;
  var query = "Insert into logErros(erro) values (?)"
  db.query(query, [logErro], (err, rows) =>{
    if (err) {
      console.error('Falha ao adiconar log', err);
      return res.status(500).json({ error: 'Falha ao adiconar log' + err.message });
    }
    res.status(201).json({ message: 'Log adicionado!' });
  })
})

//Buscar logs
app.get("/buscarLogs", function(req,res){
  var query = "Select * from logErros"
  db.query(query, (err, rows) =>{
    if (err) {
      console.error('Falha ao buscar logs', err);
      return res.status(500).json({ error: 'Falha ao buscar logs' + err.message });
    }
    console.log('Logs:', rows);
    res.json(rows); 
  })
})


app.get("/", function (req, res) {
  res.send("Server is running!");
});


//Descriptografar
function Descriptografar(dado, chave) {
  var dadoCriptoSplit = dado.split('');
  var chaveSplit = chave.split('');
  var keyCodes = new Array(chaveSplit.length);
  var keyCodeDadoCript;
  var dadoDescriptChar;
  var dadoDescriptografado = "";

  for (var i = 0; i < chaveSplit.length; i++) {
    keyCodes[i] = chaveSplit[i].charCodeAt(0);
  }

  for (var i = 0, j = 0; i < dadoCriptoSplit.length; i++) {
    keyCodeDadoCript = dadoCriptoSplit[i].charCodeAt(0);
    keyCodeDadoCript -= keyCodes[j];
    j++;

    if (j > keyCodes.length - 1) {
      j = 0;
      keyCodeDadoCript -= keyCodes[j];
    } else {
      keyCodeDadoCript -= keyCodes[j];
    }

    if (keyCodeDadoCript < 32) {
      keyCodeDadoCript += 223;
    }

    dadoDescriptChar = String.fromCharCode(keyCodeDadoCript);
    dadoDescriptografado += dadoDescriptChar;
  }

  return dadoDescriptografado;
}

//Criptografar
function Criptografar(dado, chave) {
  var dadoCriptoSplit = dado.split('');
  var chaveSplit = chave.split('');
  var keyCodes = []; 
  var keyCodeDadoCript;
  var dadoCriptChar;
  var dadoCriptografado = "";
  
  for (var i = 0; i < chaveSplit.length; i++) {
    keyCodes[i] = chaveSplit[i].charCodeAt(0);
  }

  for (var i = 0, j = 0; i < dadoCriptoSplit.length; i++) {
    keyCodeDadoCript = dadoCriptoSplit[i].charCodeAt(0);
    keyCodeDadoCript += keyCodes[j];
    j++;

    if (j > keyCodes.length - 1) {
      j = 0;
      keyCodeDadoCript += keyCodes[j];
    } else {
      keyCodeDadoCript += keyCodes[j];
    }

    if (keyCodeDadoCript > 255) {
      keyCodeDadoCript -= 223;
    }

    dadoCriptChar = String.fromCharCode(keyCodeDadoCript);
    dadoCriptografado += dadoCriptChar;
  }

  return dadoCriptografado;
}




// Inicializar o Servidor
app.listen(port, () => {
  console.log(`Server running on https://uberproject-cjajhtamdkemhqd2.canadacentral-01.azurewebsites.net:${port}`);
});
