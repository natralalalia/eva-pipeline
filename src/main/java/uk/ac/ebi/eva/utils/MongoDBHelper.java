/*
 * Copyright 2016 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.ebi.eva.utils;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;
import org.opencb.commons.utils.CryptoUtils;
import org.opencb.datastore.core.ObjectMap;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;

import uk.ac.ebi.eva.pipeline.configuration.JobParametersNames;

import java.net.UnknownHostException;

/**
 * Utility class dealing with MongoDB connections using pipeline options
 */
public class MongoDBHelper {

    public static MongoOperations getMongoOperationsFromPipelineOptions(ObjectMap pipelineOptions) {
        MongoTemplate mongoTemplate;
        try {
            mongoTemplate = getMongoTemplate(pipelineOptions);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to initialize mongo template", e);
        }
        return mongoTemplate;
    }

    private static MongoTemplate getMongoTemplate(ObjectMap pipelineOptions) throws UnknownHostException {
        MongoTemplate mongoTemplate;
        if (pipelineOptions.getString(JobParametersNames.CONFIG_DB_AUTHENTICATIONDB).isEmpty()) {
            mongoTemplate = ConnectionHelper.getMongoTemplate(
                    pipelineOptions.getString(JobParametersNames.DB_NAME)
            );
        } else {
            mongoTemplate = ConnectionHelper.getMongoTemplate(
                    pipelineOptions.getString(JobParametersNames.DB_NAME),
                    pipelineOptions.getString(JobParametersNames.CONFIG_DB_HOSTS),
                    pipelineOptions.getString(JobParametersNames.CONFIG_DB_AUTHENTICATIONDB),
                    pipelineOptions.getString(JobParametersNames.CONFIG_DB_USER),
                    pipelineOptions.getString(JobParametersNames.CONFIG_DB_PASSWORD).toCharArray()
            );
        }

        mongoTemplate.setReadPreference(getMongoTemplateReadPreferences(pipelineOptions.getString(JobParametersNames.CONFIG_DB_READPREFERENCE)));

        return mongoTemplate;
    }

    public static Mongo getMongoClientFromPipelineOptions(ObjectMap pipelineOptions) {
        Mongo mongo;
        try {
            mongo = getMongoClient(pipelineOptions);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to initialize mongo client", e);
        }

        return mongo;
    }

    private static Mongo getMongoClient(ObjectMap pipelineOptions) throws UnknownHostException {
        MongoClient mongoClient;

        if (pipelineOptions.getString(JobParametersNames.CONFIG_DB_AUTHENTICATIONDB).isEmpty()) {
            mongoClient = ConnectionHelper.getMongoClient();
        } else {
            mongoClient = ConnectionHelper.getMongoClient(
                    pipelineOptions.getString(JobParametersNames.CONFIG_DB_HOSTS),
                    pipelineOptions.getString(JobParametersNames.CONFIG_DB_AUTHENTICATIONDB),
                    pipelineOptions.getString(JobParametersNames.CONFIG_DB_USER),
                    pipelineOptions.getString(JobParametersNames.CONFIG_DB_PASSWORD).toCharArray()
            );
        }

        mongoClient.setReadPreference(getMongoTemplateReadPreferences(pipelineOptions.getString(JobParametersNames.CONFIG_DB_READPREFERENCE)));

        return mongoClient;
    }


    private static ReadPreference getMongoTemplateReadPreferences(String readPreference) {
        switch (readPreference) {
            case "primary":
                return ReadPreference.primary();
            case "secondary":
                return ReadPreference.secondary();
            default:
                throw new IllegalArgumentException(
                        String.format("%s is not a valid ReadPreference type, please use \"primary\" or \"secondary\"",
                                readPreference));
        }

    }

    /**
     * From org.opencb.opencga.storage.mongodb.variant.DBObjectToVariantConverter
     * #buildStorageId(java.lang.String, int, java.lang.String, java.lang.String)
     * <p>
     * To avoid the initialization of:
     * - DBObjectToVariantSourceEntryConverter
     * - DBObjectToVariantConverter
     */
    public static String buildStorageId(String chromosome, int start, String reference, String alternate) {
        StringBuilder builder = new StringBuilder(chromosome);
        builder.append("_");
        builder.append(start);
        builder.append("_");
        if (!reference.equals("-")) {
            if (reference.length() < 50) {
                builder.append(reference);
            } else {
                builder.append(new String(CryptoUtils.encryptSha1(reference)));
            }
        }

        builder.append("_");
        if (!alternate.equals("-")) {
            if (alternate.length() < 50) {
                builder.append(alternate);
            } else {
                builder.append(new String(CryptoUtils.encryptSha1(alternate)));
            }
        }

        return builder.toString();
    }

}
