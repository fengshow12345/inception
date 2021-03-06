/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.PrimitiveUimaFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.search.PrimitiveUimaIndexingSupport;
import mtas.analysis.token.MtasToken;
import mtas.analysis.token.MtasTokenCollection;

public class MtasUimaParserTest
{
    private Project project;
    private @Mock AnnotationSchemaService annotationSchemaService;
    private FeatureSupportRegistryImpl featureSupportRegistry;
    private FeatureIndexingSupportRegistryImpl featureIndexingSupportRegistry;
    private JCas jcas;
    
    @Before
    public void setup() throws Exception
    {
        initMocks(this);
        
        project = new Project();
        project.setId(1l);
        project.setName("test project");
        
        featureSupportRegistry = new FeatureSupportRegistryImpl(
                asList(new PrimitiveUimaFeatureSupport()));
        featureSupportRegistry.init();
        
        featureIndexingSupportRegistry = new FeatureIndexingSupportRegistryImpl(
                asList(new PrimitiveUimaIndexingSupport(featureSupportRegistry)));
        featureIndexingSupportRegistry.init();
        
        // Resetting the JCas is faster than re-creating it
        if (jcas == null) {
            jcas = JCasFactory.createJCas();
        }
        else {
            jcas.reset();
        }
    }
    
    @Test
    public void testSentencesAndTokens() throws Exception
    {
        TokenBuilder<Token, Sentence> builder = TokenBuilder.create(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test . \n This is sentence two .");

        // Only tokens and sentences here, no extra layers
        when(annotationSchemaService.listAnnotationLayer(project)).thenReturn(asList());
        
        MtasUimaParser sut = new MtasUimaParser(project, annotationSchemaService,
                featureIndexingSupportRegistry);
        MtasTokenCollection tc = sut.createTokenCollection(jcas);
        
        MtasUtils.print(tc);
        
        List<MtasToken> tokens = new ArrayList<>();
        tc.iterator().forEachRemaining(tokens::add);
        
        assertThat(tokens)
                .filteredOn(t -> "Token".equals(t.getPrefix()))
                .extracting(MtasToken::getPostfix)
                .containsExactly(
                        "This", "is", "a", "test", ".", "This", "is", "sentence", "two", ".");

        assertThat(tokens)
                .filteredOn(t -> "s".equals(t.getPrefix()))
                .extracting(MtasToken::getPostfix)
                .containsExactly(
                        "This is a test .", "This is sentence two .");
    }
    
    @Test
    public void testNamedEnity() throws Exception
    {
        JCasBuilder builder = new JCasBuilder(jcas);
        builder.add("I", Token.class);
        builder.add(" ");
        builder.add("am", Token.class);
        builder.add(" ");
        int begin = builder.getPosition();
        builder.add("John", Token.class);
        builder.add(" ");
        builder.add("Smith", Token.class);
        NamedEntity ne = new NamedEntity(jcas, begin, builder.getPosition());
        ne.setValue("PER");
        ne.addToIndexes();
        builder.add(" ");
        builder.add(".", Token.class);
        
        AnnotationLayer layer = new AnnotationLayer(NamedEntity.class.getName(),
                "Named Entity", WebAnnoConst.SPAN_TYPE, project, true);
        when(annotationSchemaService.listAnnotationLayer(any(Project.class)))
                .thenReturn(asList(layer));

        when(annotationSchemaService.listAnnotationFeature(any(AnnotationLayer.class)))
                .thenReturn(asList(
                        new AnnotationFeature(1l, layer, "value", CAS.TYPE_NAME_STRING),
                        new AnnotationFeature(2l, layer, "identifier", CAS.TYPE_NAME_STRING)));
        
        MtasUimaParser sut = new MtasUimaParser(project, annotationSchemaService,
                featureIndexingSupportRegistry);
        MtasTokenCollection tc = sut.createTokenCollection(jcas);
        
        MtasUtils.print(tc);
        
        List<MtasToken> tokens = new ArrayList<>();
        tc.iterator().forEachRemaining(tokens::add);

        assertThat(tokens)
            .filteredOn(t -> t.getPrefix().startsWith("Named_Entity"))
            .extracting(MtasToken::getPrefix)
            .containsExactly("Named_Entity", "Named_Entity.value");

        assertThat(tokens)
            .filteredOn(t -> t.getPrefix().startsWith("Named_Entity"))
            .extracting(MtasToken::getPostfix)
            .containsExactly("", "PER");
    }
}
