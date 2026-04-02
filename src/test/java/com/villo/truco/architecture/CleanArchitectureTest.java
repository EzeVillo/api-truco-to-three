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
  static final ArchRule truco_layered_architecture_must_be_respected = layeredArchitecture().consideringOnlyDependenciesInLayers()
      .layer("Domain").definedBy("com.villo.truco.domain..").layer("Application")
      .definedBy("com.villo.truco.application..").layer("Infrastructure")
      .definedBy("com.villo.truco.infrastructure..").whereLayer("Domain")
      .mayOnlyBeAccessedByLayers("Application", "Infrastructure").whereLayer("Application")
      .mayOnlyBeAccessedByLayers("Infrastructure").whereLayer("Infrastructure")
      .mayNotBeAccessedByAnyLayer();

  @ArchTest
  static final ArchRule auth_layered_architecture_must_be_respected = layeredArchitecture().consideringOnlyDependenciesInLayers()
      .layer("AuthDomain").definedBy("com.villo.truco.auth.domain..").layer("AuthApplication")
      .definedBy("com.villo.truco.auth.application..").layer("AuthInfrastructure")
      .definedBy("com.villo.truco.auth.infrastructure..").whereLayer("AuthDomain")
      .mayOnlyBeAccessedByLayers("AuthApplication", "AuthInfrastructure")
      .whereLayer("AuthApplication").mayOnlyBeAccessedByLayers("AuthInfrastructure")
      .whereLayer("AuthInfrastructure").mayNotBeAccessedByAnyLayer();

  @ArchTest
  static final ArchRule truco_domain_must_not_depend_on_application_or_infrastructure_or_spring = ArchRuleDefinition.noClasses()
      .that().resideInAPackage("com.villo.truco.domain..").should().dependOnClassesThat()
      .resideInAnyPackage("com.villo.truco.application..", "com.villo.truco.infrastructure..",
          "org.springframework..");

  @ArchTest
  static final ArchRule auth_domain_must_not_depend_on_application_or_infrastructure_or_spring = ArchRuleDefinition.noClasses()
      .that().resideInAPackage("com.villo.truco.auth.domain..").should().dependOnClassesThat()
      .resideInAnyPackage("com.villo.truco.auth.application..",
          "com.villo.truco.auth.infrastructure..", "org.springframework..");

  @ArchTest
  static final ArchRule truco_application_must_not_depend_on_spring = ArchRuleDefinition.noClasses()
      .that().resideInAPackage("com.villo.truco.application..").should().dependOnClassesThat()
      .resideInAnyPackage("org.springframework..");

  @ArchTest
  static final ArchRule auth_application_must_not_depend_on_spring = ArchRuleDefinition.noClasses()
      .that().resideInAPackage("com.villo.truco.auth.application..").should().dependOnClassesThat()
      .resideInAnyPackage("org.springframework..");

  @ArchTest
  static final ArchRule application_and_infrastructure_must_not_depend_on_chat_message_entity = ArchRuleDefinition.noClasses()
      .that().resideInAnyPackage("..application..", "..infrastructure..").should()
      .dependOnClassesThat()
      .haveFullyQualifiedName("com.villo.truco.domain.model.chat.ChatMessage");

  @ArchTest
  static final ArchRule application_must_not_depend_on_chat_persistence_snapshots = ArchRuleDefinition.noClasses()
      .that().resideInAPackage("..application..").should().dependOnClassesThat()
      .haveFullyQualifiedName("com.villo.truco.domain.model.chat.ChatSnapshot").orShould()
      .dependOnClassesThat()
      .haveFullyQualifiedName("com.villo.truco.domain.model.chat.ChatMessageSnapshot");

  @ArchTest
  static final ArchRule truco_http_layer_must_use_input_ports_not_usecase_implementations = ArchRuleDefinition.noClasses()
      .that().resideInAPackage("com.villo.truco.infrastructure.http..").should()
      .dependOnClassesThat().resideInAPackage("..application.usecases..");

  @ArchTest
  static final ArchRule auth_http_layer_must_use_input_ports_not_usecase_implementations = ArchRuleDefinition.noClasses()
      .that().resideInAPackage("com.villo.truco.auth.infrastructure.http..").should()
      .dependOnClassesThat().resideInAPackage("..auth.application.usecases..");

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
