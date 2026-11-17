# Câmera substituída por um JPEG de exemplo no cartão SD

O Wokwi não simula câmera (nem a ESP32-CAM: a placa existe, mas os pinos do sensor não são expostos). Para que o fluxo completo rode na simulação, o firmware lê um JPEG fictício de placa de Petri do cartão microSD simulado no ponto onde, no hardware real, chamaria a câmera OV2640. Sensores, iluminação, regra local, Wi-Fi, HTTP, JSON e status rodam de verdade. Na apresentação, nunca afirmar que a câmera funciona no Wokwi.

No plano gratuito, o Wokwi só copia para o cartão os arquivos de texto do projeto. Por isso o JPEG fica guardado como texto Base64 e vai assim, dentro do JSON da captura; o servidor decodifica e salva o JPEG. A imagem é um desenho simulado de placa de Petri, distinguível e legível (não precisa ser foto real), com menos de 30 KB, e o Base64 é gerado a partir de um JPEG real e válido, para que nenhuma imagem chegue corrompida.
