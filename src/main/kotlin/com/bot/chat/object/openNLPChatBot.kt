package com.bot.chat

import opennlp.tools.doccat.*
import opennlp.tools.lemmatizer.LemmatizerME
import opennlp.tools.lemmatizer.LemmatizerModel
import opennlp.tools.postag.POSModel
import opennlp.tools.postag.POSTaggerME
import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import opennlp.tools.util.*
import opennlp.tools.util.model.ModelUtil
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors

/**
 * Custom chat bot or chat agent for automated chat replies for FAQs. It uses
 * different features of Apache OpenNLP for understanding what user is asking
 * for. NLP is natural language processing.
 */
object openNLPChatBot {

    private val questionAnswer = Properties()
    private val logger = getLogger(openNLPChatBot::class.java)

	fun answer(question: String): Pair<String, Boolean> {
        // Train categorizer model to the training data we created.
        val model = trainCategorizerModel()
        // Take chat inputs from console (user) in a loop.
        logger.debug("##### You:")
        // Break users chat input into sentences using sentence detection.
        val sentences = breakSentences(question)
        var answer = ""
        var conversationComplete = false
        // Loop through sentences.
        for (sentence in sentences) { // Separate words from each sentence using tokenizer.
            val tokens = tokenizeSentence(sentence)
            // Tag separated words with POS tags to understand their gramatical structure.
            val posTags = detectPOSTags(tokens)
            // Lemmatize each word so that its easy to categorize.
            val lemmas = lemmatizeTokens(tokens, posTags)
            // Determine BEST category using lemmatized tokens used a mode that we trained at start.
            val category = detectCategory(model, lemmas)
            // Get predefined answer from given category & add to answer.
            answer = answer + " " + questionAnswer[category]
            // If category conversation-complete, we will end chat conversation.
			if ("conversation-complete" == category) {
                conversationComplete = true
            }
        }
        // Print answer back to user. If conversation is marked as complete, then end loop & program.
        logger.debug("##### Chat Bot: {}", answer)
        return Pair(answer, conversationComplete)
    }

    /**
     * Train categorizer model as per the category sample training data we created.
     *
     * @return
     */
    private fun trainCategorizerModel(): DoccatModel {
        // faq-categorizer.txt is a custom training data with categories as per our chat requirements.
        val inputStreamFactory: InputStreamFactory = MarkableFileInputStreamFactory(File(javaClass.getResource(
            "/faq-categorizer.txt").toURI()))
        val lineStream: ObjectStream<String> = PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8)
        val sampleStream: ObjectStream<DocumentSample> = DocumentSampleStream(lineStream)
        val factory = DoccatFactory(arrayOf<FeatureGenerator>(BagOfWordsFeatureGenerator()))
        val params = ModelUtil.createDefaultTrainingParameters()
        params.put(TrainingParameters.CUTOFF_PARAM, 0)
        // Train a model with classifications from above file.
        return DocumentCategorizerME.train("en", sampleStream, params, factory)
    }

    /**
     * Detect category using given token. Use categorizer feature of Apache OpenNLP.
     *
     * @param model
     * @param finalTokens
     * @return
     */
    private fun detectCategory(model: DoccatModel, finalTokens: Array<String>): String {
        // Initialize document categorizer tool
        val myCategorizer = DocumentCategorizerME(model)
        // Get best possible category.
        val probabilitiesOfOutcomes = myCategorizer.categorize(finalTokens)
        val category = myCategorizer.getBestCategory(probabilitiesOfOutcomes)
        logger.debug("Category: {}", category)
        return category
    }

    /**
     * Break data into sentences using sentence detection feature of Apache OpenNLP.
     *
     * @param data
     * @return
     */
    private fun breakSentences(data: String): Array<String> {
        // Better to read file once at start of program & store model in instance variable.
        // But keeping here for simplicity in understanding.
        FileInputStream(javaClass.getResource("/en-sent.bin").file).use { modelIn ->
            val myCategorizer = SentenceDetectorME(SentenceModel(modelIn))
            val sentences = myCategorizer.sentDetect(data)
            logger.debug("Sentence Detection: {}", Arrays.stream(sentences).collect(Collectors.joining(" | ")))
            return sentences
        }
    }

    /**
     * Break sentence into words & punctuation marks using tokenizer feature of
     * Apache OpenNLP.
     *
     * @param sentence
     * @return
     */
    private fun tokenizeSentence(sentence: String): Array<String> {
        // Better to read file once at start of program & store model in instance variable.
        // But keeping here for simplicity in understanding.
        FileInputStream(javaClass.getResource("/en-token.bin").file).use { modelIn ->
            // Initialize tokenizer tool
            val myCategorizer = TokenizerME(TokenizerModel(modelIn))
            // Tokenize sentence.
            val tokens = myCategorizer.tokenize(sentence)
            logger.debug("Tokenizer : {}", Arrays.stream(tokens).collect(Collectors.joining(" | ")))
            return tokens
        }
    }

    /**
     * Find part-of-speech or POS tags of all tokens using POS tagger feature of
     * Apache OpenNLP.
     *
     * @param tokens
     * @return
     */
    private fun detectPOSTags(tokens: Array<String>): Array<String> {
        // Better to read file once at start of program & store model in instance variable.
        // But keeping here for simplicity in understanding.
        FileInputStream(javaClass.getResource("/en-pos-maxent.bin").file).use { modelIn ->
            // Initialize POS tagger tool
            val myCategorizer = POSTaggerME(POSModel(modelIn))
            // Tag sentence.
            val posTokens = myCategorizer.tag(tokens)
            logger.debug("POS Tags : {}", Arrays.stream(posTokens).collect(Collectors.joining(" | ")))
            return posTokens
        }
    }

    /**
     * Find lemma of tokens using lemmatizer feature of Apache OpenNLP.
     *
     * @param tokens
     * @param posTags
     * @return
     */
    private fun lemmatizeTokens(tokens: Array<String>, posTags: Array<String>): Array<String> {
        // Better to read file once at start of program & store model in instance variable.
        // But keeping here for simplicity in understanding.
        FileInputStream(javaClass.getResource("/en-lemmatizer.bin").file).use { modelIn ->
            // Tag sentence.
            val myCategorizer = LemmatizerME(LemmatizerModel(modelIn))
            val lemmaTokens = myCategorizer.lemmatize(tokens, posTags)
            logger.debug("Lemmatizer : {}", Arrays.stream(lemmaTokens).collect(Collectors.joining(" | ")))
            return lemmaTokens
        }
    }

    /*
	 * Define answers for each given category.
	 */
    init {
        val questionAnswerFile = "/questionAnswer.properties"
        val stream = javaClass.getResource(questionAnswerFile).openStream();
        questionAnswer.load(stream)
    }
}