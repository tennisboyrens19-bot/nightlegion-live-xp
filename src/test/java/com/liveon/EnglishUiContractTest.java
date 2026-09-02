package com.liveon;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class EnglishUiContractTest
{
    @Test
    public void activeJavaHasNoKnownVisiblePortugueseFragments() throws Exception
    {
        Path root = Paths.get("src", "main", "java");
        List<String> blocked = Arrays.asList(
            "\"Dourado\"", "\"Vermelho\"", "\"Azul\"", "\"Verde\"",
            "\"Roxo\"", "\"Branco\"", "new JLabel(\"Cor\")",
            "MELHORES TEMPOS", "Colaborador", "Administrador", "Channel associado",
            "Promo\\u00E7\\u00E3o publicada", "Home principal", "Rankings MVP",
            "New clan best time em", "+ \"º\""
        );
        try (Stream<Path> paths = Files.walk(root))
        {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java"))
                .collect(Collectors.toList()))
            {
                String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                for (String fragment : blocked)
                {
                    assertFalse(path + " contains visible Portuguese fragment " + fragment,
                        source.contains(fragment));
                }
            }
        }
    }
}
