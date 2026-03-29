package com.villo.truco.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

@AnalyzeClasses(packages = "com.villo.truco", importOptions = ImportOption.DoNotIncludeTests.class)
class CleanArchitectureTest {

  @ArchTest
  static final ArchRule layered_architecture_must_be_respected = layeredArchitecture().consideringOnlyDependenciesInLayers()
      .layer("Domain").definedBy("..domain..").layer("Application").definedBy("..application..")
      .layer("Infrastructure").definedBy("..infrastructure..").whereLayer("Domain")
      .mayOnlyBeAccessedByLayers("Application", "Infrastructure").whereLayer("Application")
      .mayOnlyBeAccessedByLayers("Infrastructure").whereLayer("Infrastructure")
      .mayNotBeAccessedByAnyLayer();

  @ArchTest
  static final ArchRule domain_must_not_depend_on_application_or_infrastructure_or_spring = ArchRuleDefinition.noClasses()
      .that().resideInAPackage("..domain..").should().dependOnClassesThat()
      .resideInAnyPackage("..application..", "..infrastructure..", "org.springframework..");

  @ArchTest
  static final ArchRule application_must_not_depend_on_spring = ArchRuleDefinition.noClasses()
      .that().resideInAPackage("..application..").should().dependOnClassesThat()
      .resideInAnyPackage("org.springframework..");

  @ArchTest
  static final ArchRule http_layer_must_use_input_ports_not_usecase_implementations = ArchRuleDefinition.noClasses()
      .that().resideInAPackage("..infrastructure.http..").should().dependOnClassesThat()
      .resideInAPackage("..application.usecases..");

  @ArchTest
  static final ArchRule bot_domain_must_not_depend_on_match_domain = ArchRuleDefinition.noClasses()
      .that().resideInAPackage("..domain.model.bot..").should().dependOnClassesThat()
      .resideInAPackage("..domain.model.match..");

  @ArchTest
  static final ArchRule bot_domain_may_only_use_cards_shared_kernel = ArchRuleDefinition.noClasses()
      .that().resideInAPackage("..domain.model.bot..").and().doNotHaveSimpleName("BotCard").should()
      .dependOnClassesThat().resideInAPackage("..domain.cards..")
      .because("only BotCard may depend on the domain.cards shared kernel");

}
