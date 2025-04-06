package br.fecap.teste.network;

public class Criptografia {

    public static String Criptografar(String senha, String email) {
        char[] senhaSplit = senha.toCharArray();
        char[] emailSplit = email.toCharArray();
        int[] keyCodes = new int[emailSplit.length];
        int keyCodeSenha;
        char senhaCriptChar;
        String senhaCriptografada ="";

        for(int i = 0; i<emailSplit.length; i++) {
            keyCodes[i] = emailSplit[i];
        }
        for(int i=0, j=0;i<senhaSplit.length;i++) {
            keyCodeSenha = senhaSplit[i];
            keyCodeSenha += keyCodes[j];
            j++;
            if(j > keyCodes.length -1) {
                j = 0;
                keyCodeSenha += keyCodes[j];
            }else {
                keyCodeSenha += keyCodes[j];
            }
            if(keyCodeSenha > 255) {
                keyCodeSenha -=223;
            }
            senhaCriptChar = (char) keyCodeSenha;
            senhaCriptografada += senhaCriptChar;
        }
        return senhaCriptografada;
    }
}
