package com.example.parsingexpression

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.google.mlkit.nl.entityextraction.*
import java.text.DateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "Main"
        private const val CURRENT_MODEL_KEY = "current_model_key"
        const val REQUEST_CODE = 1
        val output = mutableListOf<String>()
        private fun getEntityExtractionParams(input: String): EntityExtractionParams {
            return EntityExtractionParams.Builder(input).build()
        }
    }


    private var currentModel: String = EntityExtractorOptions.ENGLISH

    private lateinit var entityExtractor: EntityExtractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentModel = savedInstanceState?.getString(CURRENT_MODEL_KEY, EntityExtractorOptions.ENGLISH)
            ?: EntityExtractorOptions.ENGLISH

        val options = EntityExtractorOptions.Builder(currentModel).build()
        entityExtractor = EntityExtraction.getClient(options)
        lifecycle.addObserver(entityExtractor)

        fun convertGranularityToString(entity: Entity): String {
            val dateTimeEntity = entity.asDateTimeEntity()
            return when (dateTimeEntity!!.dateTimeGranularity) {
                DateTimeEntity.GRANULARITY_YEAR -> getString(R.string.granularity_year)
                DateTimeEntity.GRANULARITY_MONTH -> getString(R.string.granularity_month)
                DateTimeEntity.GRANULARITY_WEEK -> getString(R.string.granularity_week)
                DateTimeEntity.GRANULARITY_DAY -> getString(R.string.granularity_day)
                DateTimeEntity.GRANULARITY_HOUR -> getString(R.string.granularity_hour)
                DateTimeEntity.GRANULARITY_MINUTE -> getString(R.string.granularity_minute)
                DateTimeEntity.GRANULARITY_SECOND -> getString(R.string.granularity_second)
                else -> getString(R.string.granularity_unknown)
            }
        }

        fun displayEmailInfo(annotatedText: String) {
            output.add(getString(R.string.email_entity_info, annotatedText))

        }

        fun displayPhoneInfo(annotatedText: String) {
            output.add(
                getString(
                    R.string.phone_entity_info_formatted,
                    annotatedText,
                    PhoneNumberUtils.formatNumber(annotatedText)
                )
            )
        }

        fun displayDefaultInfo(annotatedText: String) {
            output.add(getString(R.string.unknown_entity_info, annotatedText))
        }

        fun displayUrlInfo(annotatedText: String) {
            output.add(getString(R.string.url_entity_info, annotatedText))
        }

        fun displayDateTimeInfo(entity: Entity, annotatedText: String) {
            val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG)
                .format(Date(entity.asDateTimeEntity()!!.timestampMillis))
            output.add(
                getString(
                    R.string.date_time_entity_info,
                    annotatedText,
                    dateTimeFormat,
                    convertGranularityToString(entity)
                )
            )
        }

        fun displayAddressInfo(annotatedText: String) {
            output.add(getString(R.string.address_entity_info, annotatedText))
        }

        fun displayTrackingNoInfo(entity: Entity, annotatedText: String) {
            val trackingNumberEntity = entity.asTrackingNumberEntity()
            output.add(
                getString(
                    R.string.tracking_number_entity_info,
                    annotatedText,
                    trackingNumberEntity!!.parcelCarrier,
                    trackingNumberEntity.parcelTrackingNumber
                )
            )
        }

        fun displayPaymentCardInfo(entity: Entity, annotatedText: String) {
            val paymentCardEntity = entity.asPaymentCardEntity()
            output.add(
                getString(
                    R.string.payment_card_entity_info,
                    annotatedText,
                    paymentCardEntity!!.paymentCardNetwork,
                    paymentCardEntity.paymentCardNumber
                )
            )
        }

        fun displayIsbnInfo(entity: Entity, annotatedText: String) {
            output.add(
                getString(R.string.isbn_entity_info, annotatedText, entity.asIsbnEntity()!!.isbn)
            )
        }

        fun displayIbanInfo(entity: Entity, annotatedText: String) {
            val ibanEntity = entity.asIbanEntity()
            output.add(
                getString(
                    R.string.iban_entity_info,
                    annotatedText,
                    ibanEntity!!.iban,
                    ibanEntity.ibanCountryCode
                )
            )
        }

        fun displayFlightNoInfo(entity: Entity, annotatedText: String) {
            val flightNumberEntity = entity.asFlightNumberEntity()
            output.add(
                getString(
                    R.string.flight_number_entity_info,
                    annotatedText,
                    flightNumberEntity!!.airlineCode,
                    flightNumberEntity.flightNumber
                )
            )
        }

        fun displayMoneyEntityInfo(entity: Entity, annotatedText: String) {
            val moneyEntity = entity.asMoneyEntity()
            output.add(
                getString(
                    R.string.money_entity_info,
                    annotatedText,
                    moneyEntity!!.unnormalizedCurrency,
                    moneyEntity.integerPart,
                    moneyEntity.fractionalPart
                )
            )
        }

        fun displayEntityInfo(annotatedText: String, entity: Entity) {
            when (entity.type) {
                Entity.TYPE_ADDRESS -> displayAddressInfo(annotatedText)
                Entity.TYPE_DATE_TIME -> displayDateTimeInfo(entity, annotatedText)
                Entity.TYPE_EMAIL -> displayEmailInfo(annotatedText)
                Entity.TYPE_FLIGHT_NUMBER -> displayFlightNoInfo(entity, annotatedText)
                Entity.TYPE_IBAN -> displayIbanInfo(entity, annotatedText)
                Entity.TYPE_ISBN -> displayIsbnInfo(entity, annotatedText)
                Entity.TYPE_MONEY -> displayMoneyEntityInfo(entity, annotatedText)
                Entity.TYPE_PAYMENT_CARD -> displayPaymentCardInfo(entity, annotatedText)
                Entity.TYPE_PHONE -> displayPhoneInfo(annotatedText)
                Entity.TYPE_TRACKING_NUMBER -> displayTrackingNoInfo(entity, annotatedText)
                Entity.TYPE_URL -> displayUrlInfo(annotatedText)
                else -> displayDefaultInfo(annotatedText)
            }
        }



        fun extractEntities(input: String) {
            entityExtractor
                .downloadModelIfNeeded()
                .onSuccessTask {
                    entityExtractor.annotate(
                        getEntityExtractionParams(
                            input
                        )
                    )
                }
                .addOnFailureListener { e: Exception? ->
                    Log.d(TAG, "Annotation failed", )

                }
                .addOnSuccessListener { result: List<EntityAnnotation> ->
                    if (result.isEmpty()) {

                        return@addOnSuccessListener
                    }

                    for (entityAnnotation in result) {
                        val entities = entityAnnotation.entities
                        val annotatedText = entityAnnotation.annotatedText
                        for (entity in entities) {
                            displayEntityInfo(annotatedText, entity)

                        }
                    }
                    val outputString = output.joinToString("\n")
                    Log.d(TAG, "Hello, let's see:\n$outputString")
                }
        }
        extractEntities("Hello Soumyadeep lets meet tomorrow ")


    }
    }


