package com.villo.truco.profile.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.villo.truco.profile.domain.model.AchievementCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Contrato AchievementCode <-> docs/CONTRATOS_API.md")
class AchievementCatalogContractTest {

  private static final Path CONTRATOS = Path.of("docs", "CONTRATOS_API.md");
  private static final String SECTION_HEADER = "### 8.3 Logros";
  private static final Pattern CODE_LINE = Pattern.compile("^-\\s+`([A-Z0-9_]+)`\\s*$");

  @Test
  @DisplayName("la seccion 8.3 documenta exactamente los mismos codigos que el enum AchievementCode")
  void seccion83CoincideConElEnum() throws IOException {

    final Set<String> documentados = this.parseDocumentedCodes();
    final Set<String> enEnum = Arrays.stream(AchievementCode.values()).map(Enum::name)
        .collect(LinkedHashSet::new, Set::add, Set::addAll);

    assertThat(documentados)
        .as("Los codigos en docs/CONTRATOS_API.md (8.3) deben coincidir con AchievementCode. "
            + "Si agregaste o quitaste un logro, actualiza la seccion 8.3.")
        .isEqualTo(enEnum);
  }

  private Set<String> parseDocumentedCodes() throws IOException {

    final var lines = Files.readAllLines(CONTRATOS);
    final Set<String> codes = new LinkedHashSet<>();
    var insideSection = false;
    for (final var line : lines) {
      if (line.startsWith(SECTION_HEADER)) {
        insideSection = true;
        continue;
      }
      if (insideSection && line.startsWith("## ")) {
        break;
      }
      if (insideSection) {
        final Matcher matcher = CODE_LINE.matcher(line.trim());
        if (matcher.matches()) {
          codes.add(matcher.group(1));
        }
      }
    }
    return codes;
  }

}
