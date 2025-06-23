
```mermaid
erDiagram
    USERS {
        bigint ID PK
        varchar USERNAME UK
        varchar PASSWORD
        varchar NAME
        enum ROLE
        varchar CONTACT_INFO
        datetime CREATED_AT
        datetime UPDATED_AT
        datetime DELETED_AT
    }

    RAW_QUESTIONS {
        bigint ID PK
        varchar SOURCE_URL UK
        varchar SOURCE_SITE
        varchar TITLE
        text CONTENT
        datetime CRAWL_TIME
        json TAGS
        json OTHER_METADATA
    }

    RAW_ANSWERS {
        bigint ID PK
        bigint RAW_QUESTION_ID FK
        varchar AUTHOR_INFO
        text CONTENT
        datetime PUBLISH_TIME
        int UPVOTES
        boolean IS_ACCEPTED
        json OTHER_METADATA
    }

    CHANGE_LOG {
        bigint ID PK
        datetime CHANGE_TIME
        bigint CHANGED_BY_USER_ID FK
        varchar CHANGE_TYPE
        text COMMIT_MESSAGE
        bigint ASSOCIATED_STANDARD_QUESTION_ID FK
    }

    CHANGE_LOG_DETAILS {
        bigint ID PK
        bigint CHANGE_LOG_ID FK
        enum ENTITY_TYPE
        bigint ENTITY_ID
        varchar ATTRIBUTE_NAME
        json OLD_VALUE
        json NEW_VALUE
    }

    STANDARD_QUESTIONS {
        bigint ID PK
        bigint ORIGINAL_RAW_QUESTION_ID FK
        text QUESTION_TEXT
        enum QUESTION_TYPE
        enum DIFFICULTY
        datetime CREATION_TIME
        bigint CREATED_BY_USER_ID FK
        bigint PARENT_STANDARD_QUESTION_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    EXPERT_CANDIDATE_ANSWERS {
        bigint ID PK
        bigint STANDARD_QUESTION_ID FK
        bigint USER_ID FK
        text CANDIDATE_ANSWER_TEXT
        datetime SUBMISSION_TIME
        int QUALITY_SCORE
        text FEEDBACK
    }

    CROWDSOURCED_ANSWERS {
        bigint ID PK
        bigint STANDARD_QUESTION_ID FK
        bigint USER_ID FK
        text ANSWER_TEXT
        datetime SUBMISSION_TIME
        bigint TASK_BATCH_ID
        enum QUALITY_REVIEW_STATUS
        bigint REVIEWED_BY_USER_ID FK
        datetime REVIEW_TIME
        text REVIEW_FEEDBACK
        json OTHER_METADATA
    }

    STANDARD_OBJECTIVE_ANSWERS {
        bigint ID PK
        bigint STANDARD_QUESTION_ID FK
        json OPTIONS
        json CORRECT_IDS
        bigint DETERMINED_BY_USER_ID FK
        datetime DETERMINED_TIME
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    STANDARD_SIMPLE_ANSWERS {
        bigint ID PK
        bigint STANDARD_QUESTION_ID FK
        text ANSWER_TEXT
        json ALTERNATIVE_ANSWERS
        bigint DETERMINED_BY_USER_ID FK
        datetime DETERMINED_TIME
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    STANDARD_SUBJECTIVE_ANSWERS {
        bigint ID PK
        bigint STANDARD_QUESTION_ID FK
        text ANSWER_TEXT
        text SCORING_GUIDANCE
        bigint DETERMINED_BY_USER_ID FK
        datetime DETERMINED_TIME
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    TAGS {
        bigint ID PK
        varchar TAG_NAME UK
        varchar TAG_TYPE
        text DESCRIPTION
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    STANDARD_QUESTION_TAGS {
        bigint ID PK
        bigint STANDARD_QUESTION_ID FK
        bigint TAG_ID FK
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
    }

    RAW_QUESTION_TAGS {
        bigint ID PK
        bigint RAW_QUESTION_ID FK
        bigint TAG_ID FK
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
    }

    ANSWER_TAG_PROMPTS {
        bigint ID PK
        bigint TAG_ID FK
        varchar NAME
        text PROMPT_TEMPLATE
        text DESCRIPTION
        boolean IS_ACTIVE
        int PROMPT_PRIORITY
        varchar VERSION
        datetime CREATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint PARENT_PROMPT_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    ANSWER_QUESTION_TYPE_PROMPTS {
        bigint ID PK
        varchar NAME
        enum QUESTION_TYPE
        text PROMPT_TEMPLATE
        text DESCRIPTION
        boolean IS_ACTIVE
        text RESPONSE_FORMAT_INSTRUCTION
        text RESPONSE_EXAMPLE
        varchar VERSION
        datetime CREATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint PARENT_PROMPT_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    ANSWER_PROMPT_ASSEMBLY_CONFIGS {
        bigint ID PK
        varchar NAME
        text DESCRIPTION
        text BASE_SYSTEM_PROMPT
        varchar TAG_PROMPTS_SECTION_HEADER
        varchar QUESTION_TYPE_SECTION_HEADER
        varchar TAG_PROMPT_SEPARATOR
        varchar SECTION_SEPARATOR
        text FINAL_INSTRUCTION
        boolean IS_ACTIVE
        datetime CREATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
    }

    EVALUATION_TAG_PROMPTS {
        bigint ID PK
        bigint TAG_ID FK
        varchar NAME
        text PROMPT_TEMPLATE
        text DESCRIPTION
        boolean IS_ACTIVE
        int PROMPT_PRIORITY
        varchar VERSION
        datetime CREATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint PARENT_PROMPT_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    EVALUATION_SUBJECTIVE_PROMPTS {
        bigint ID PK
        varchar NAME
        text PROMPT_TEMPLATE
        text DESCRIPTION
        json EVALUATION_CRITERIA_FOCUS
        text SCORING_INSTRUCTION
        text OUTPUT_FORMAT_INSTRUCTION
        boolean IS_ACTIVE
        varchar VERSION
        datetime CREATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint PARENT_PROMPT_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    EVALUATION_PROMPT_ASSEMBLY_CONFIGS {
        bigint ID PK
        varchar NAME
        text DESCRIPTION
        text BASE_SYSTEM_PROMPT
        varchar TAG_PROMPTS_SECTION_HEADER
        varchar SUBJECTIVE_SECTION_HEADER
        varchar TAG_PROMPT_SEPARATOR
        varchar SECTION_SEPARATOR
        text FINAL_INSTRUCTION
        boolean IS_ACTIVE
        datetime CREATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
    }

    DATASET_VERSIONS {
        bigint ID PK
        varchar VERSION_NUMBER UK
        varchar NAME
        text DESCRIPTION
        datetime CREATION_TIME
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    DATASET_QUESTION_MAPPING {
        bigint ID PK
        bigint DATASET_VERSION_ID FK
        bigint STANDARD_QUESTION_ID FK
        int ORDER_IN_DATASET
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
    }

    LLM_MODELS {
        bigint ID PK
        varchar NAME
        varchar PROVIDER
        varchar VERSION
        text DESCRIPTION
        varchar API_URL
        varchar API_KEY
        varchar API_TYPE
        json MODEL_PARAMETERS
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    ANSWER_GENERATION_BATCHES {
        bigint ID PK
        varchar NAME
        text DESCRIPTION
        bigint DATASET_VERSION_ID FK
        datetime CREATION_TIME
        enum STATUS
        bigint ANSWER_ASSEMBLY_CONFIG_ID FK
        bigint SINGLE_CHOICE_PROMPT_ID FK
        bigint MULTIPLE_CHOICE_PROMPT_ID FK
        bigint SIMPLE_FACT_PROMPT_ID FK
        bigint SUBJECTIVE_PROMPT_ID FK
        json GLOBAL_PARAMETERS
        bigint CREATED_BY_USER_ID FK
        datetime COMPLETED_AT
        decimal PROGRESS_PERCENTAGE
        datetime LAST_ACTIVITY_TIME
        datetime LAST_CHECK_TIME
        int RESUME_COUNT
        datetime PAUSE_TIME
        text PAUSE_REASON
        int ANSWER_REPEAT_COUNT
        text ERROR_MESSAGE
        varchar PROCESSING_INSTANCE
        bigint LAST_PROCESSED_RUN_ID FK
    }

    MODEL_ANSWER_RUNS {
        bigint ID PK
        bigint ANSWER_GENERATION_BATCH_ID FK
        bigint LLM_MODEL_ID FK
        varchar RUN_NAME
        text RUN_DESCRIPTION
        int RUN_INDEX
        datetime RUN_TIME
        enum STATUS
        json PARAMETERS
        text ERROR_MESSAGE
        bigint CREATED_BY_USER_ID FK
        bigint LAST_PROCESSED_QUESTION_ID FK
        int LAST_PROCESSED_QUESTION_INDEX
        decimal PROGRESS_PERCENTAGE
        datetime LAST_ACTIVITY_TIME
        int RESUME_COUNT
        datetime PAUSE_TIME
        text PAUSE_REASON
        int COMPLETED_QUESTIONS_COUNT
        int TOTAL_QUESTIONS_COUNT
        int FAILED_QUESTIONS_COUNT
        json FAILED_QUESTIONS_IDS
    }

    LLM_ANSWERS {
        bigint ID PK
        bigint MODEL_ANSWER_RUN_ID FK
        bigint DATASET_QUESTION_MAPPING_ID FK
        text ANSWER_TEXT
        enum GENERATION_STATUS
        text ERROR_MESSAGE
        datetime GENERATION_TIME
        text PROMPT_USED
        text RAW_MODEL_RESPONSE
        json OTHER_METADATA
        int REPEAT_INDEX
    }

    EVALUATION_CRITERIA {
        bigint ID PK
        varchar NAME
        varchar VERSION
        text DESCRIPTION
        enum DATA_TYPE
        varchar SCORE_RANGE
        json APPLICABLE_QUESTION_TYPES
        boolean IS_REQUIRED
        int ORDER_INDEX
        decimal WEIGHT
        enum QUESTION_TYPE
        json OPTIONS
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint PARENT_CRITERION_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    EVALUATORS {
        bigint ID PK
        enum EVALUATOR_TYPE
        bigint USER_ID FK
        bigint LLM_MODEL_ID FK
        varchar NAME
        datetime CREATED_AT
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime DELETED_AT
    }

    EVALUATION_RUNS {
        bigint ID PK
        bigint MODEL_ANSWER_RUN_ID FK
        bigint EVALUATOR_ID FK
        varchar RUN_NAME
        text RUN_DESCRIPTION
        datetime RUN_TIME
        datetime START_TIME
        datetime END_TIME
        enum STATUS
        json PARAMETERS
        bigint EVALUATION_ASSEMBLY_CONFIG_ID FK
        bigint SUBJECTIVE_PROMPT_ID FK
        text ERROR_MESSAGE
        bigint CREATED_BY_USER_ID FK
        bigint LAST_PROCESSED_ANSWER_ID FK
        decimal PROGRESS_PERCENTAGE
        datetime LAST_ACTIVITY_TIME
        int COMPLETED_ANSWERS_COUNT
        int TOTAL_ANSWERS_COUNT
        int FAILED_EVALUATIONS_COUNT
        int RESUME_COUNT
        datetime COMPLETED_AT
        text PAUSE_REASON
        datetime PAUSE_TIME
        bigint PAUSED_BY_USER_ID FK
        int TIMEOUT_SECONDS
        boolean IS_AUTO_RESUME
        int AUTO_CHECKPOINT_INTERVAL
        bigint CURRENT_BATCH_START_ID
        bigint CURRENT_BATCH_END_ID
        int BATCH_SIZE
        int RETRY_COUNT
        int MAX_RETRIES
        datetime LAST_ERROR_TIME
        int CONSECUTIVE_ERRORS
        datetime LAST_UPDATED
    }

    EVALUATIONS {
        bigint ID PK
        bigint LLM_ANSWER_ID FK
        bigint EVALUATOR_ID FK
        bigint EVALUATION_RUN_ID FK
        enum EVALUATION_TYPE
        decimal OVERALL_SCORE
        datetime EVALUATION_TIME
        enum EVALUATION_STATUS
        text ERROR_MESSAGE
        json EVALUATION_RESULTS
        text PROMPT_USED
        text COMMENTS
        text RAW_EVALUATOR_RESPONSE
        bigint CREATED_BY_USER_ID FK
        bigint CREATED_CHANGE_LOG_ID FK
        datetime CREATION_TIME
        datetime COMPLETION_TIME
        decimal RAW_SCORE
        decimal NORMALIZED_SCORE
        decimal WEIGHTED_SCORE
        varchar SCORE_TYPE
        varchar SCORING_METHOD
    }

    EVALUATION_DETAILS {
        bigint ID PK
        bigint EVALUATION_ID FK
        bigint CRITERION_ID FK
        varchar CRITERION_NAME
        decimal SCORE
        text COMMENTS
        datetime CREATED_AT
    }

    MODEL_BATCH_SCORES {
        bigint ID PK
        bigint BATCH_ID FK
        bigint MODEL_ID FK
        bigint EVALUATOR_ID FK
        varchar SCORE_TYPE
        decimal AVERAGE_SCORE
        int TOTAL_ANSWERS
        int SCORED_ANSWERS
        decimal MAX_SCORE
        decimal MIN_SCORE
        json SCORE_DISTRIBUTION
        datetime CALCULATED_AT
        datetime UPDATED_AT
        bigint CREATED_BY_USER_ID FK
        int REPEAT_INDEX
    }

    USERS ||--o{ CHANGE_LOG : user_creates
    USERS ||--o{ STANDARD_QUESTIONS : user_creates
    USERS ||--o{ EXPERT_CANDIDATE_ANSWERS : user_submits
    USERS ||--o{ CROWDSOURCED_ANSWERS : user_submits
    USERS ||--o{ CROWDSOURCED_ANSWERS : user_reviews
    USERS ||--o{ TAGS : user_creates
    USERS ||--o{ ANSWER_TAG_PROMPTS : user_creates
    USERS ||--o{ ANSWER_QUESTION_TYPE_PROMPTS : user_creates
    USERS ||--o{ ANSWER_PROMPT_ASSEMBLY_CONFIGS : user_creates
    USERS ||--o{ EVALUATION_TAG_PROMPTS : user_creates
    USERS ||--o{ EVALUATION_SUBJECTIVE_PROMPTS : user_creates
    USERS ||--o{ EVALUATION_PROMPT_ASSEMBLY_CONFIGS : user_creates
    USERS ||--o{ DATASET_VERSIONS : user_creates
    USERS ||--o{ LLM_MODELS : user_creates
    USERS ||--o{ ANSWER_GENERATION_BATCHES : user_creates
    USERS ||--o{ MODEL_ANSWER_RUNS : user_creates
    USERS ||--o{ EVALUATION_CRITERIA : user_creates
    USERS ||--o{ EVALUATORS : user_creates
    USERS ||--o{ EVALUATION_RUNS : user_creates
    USERS ||--o{ EVALUATIONS : user_creates

    RAW_QUESTIONS ||--o{ RAW_ANSWERS : question_has_answers
    RAW_QUESTIONS ||--o{ STANDARD_QUESTIONS : raw_to_standard
    RAW_QUESTIONS ||--o{ RAW_QUESTION_TAGS : raw_question_tagged

    CHANGE_LOG ||--o{ CHANGE_LOG_DETAILS : log_contains_details
    CHANGE_LOG ||--o{ STANDARD_QUESTIONS : log_tracks_questions
    CHANGE_LOG ||--o{ TAGS : log_tracks_tags
    CHANGE_LOG ||--o{ ANSWER_TAG_PROMPTS : log_tracks_prompts
    CHANGE_LOG ||--o{ EVALUATION_CRITERIA : log_tracks_criteria

    STANDARD_QUESTIONS ||--o{ EXPERT_CANDIDATE_ANSWERS : question_has_expert_answers
    STANDARD_QUESTIONS ||--o{ CROWDSOURCED_ANSWERS : question_has_crowd_answers
    STANDARD_QUESTIONS ||--o{ STANDARD_OBJECTIVE_ANSWERS : question_has_objective_answers
    STANDARD_QUESTIONS ||--o{ STANDARD_SIMPLE_ANSWERS : question_has_simple_answers
    STANDARD_QUESTIONS ||--o{ STANDARD_SUBJECTIVE_ANSWERS : question_has_subjective_answers
    STANDARD_QUESTIONS ||--o{ STANDARD_QUESTION_TAGS : question_tagged
    STANDARD_QUESTIONS ||--o{ DATASET_QUESTION_MAPPING : question_in_dataset

    TAGS ||--o{ STANDARD_QUESTION_TAGS : tag_applied_to_questions
    TAGS ||--o{ RAW_QUESTION_TAGS : tag_applied_to_raw_questions
    TAGS ||--o{ ANSWER_TAG_PROMPTS : tag_has_answer_prompts
    TAGS ||--o{ EVALUATION_TAG_PROMPTS : tag_has_eval_prompts

    ANSWER_TAG_PROMPTS ||--o{ ANSWER_TAG_PROMPTS : prompt_versioning
    ANSWER_QUESTION_TYPE_PROMPTS ||--o{ ANSWER_QUESTION_TYPE_PROMPTS : prompt_versioning
    EVALUATION_TAG_PROMPTS ||--o{ EVALUATION_TAG_PROMPTS : prompt_versioning
    EVALUATION_SUBJECTIVE_PROMPTS ||--o{ EVALUATION_SUBJECTIVE_PROMPTS : prompt_versioning

    DATASET_VERSIONS ||--o{ DATASET_QUESTION_MAPPING : dataset_contains_questions
    DATASET_VERSIONS ||--o{ ANSWER_GENERATION_BATCHES : dataset_used_in_batches

    LLM_MODELS ||--o{ MODEL_ANSWER_RUNS : model_runs_answers
    LLM_MODELS ||--o{ EVALUATORS : model_used_for_evaluation

    ANSWER_PROMPT_ASSEMBLY_CONFIGS ||--o{ ANSWER_GENERATION_BATCHES : config_used_in_batches
    ANSWER_QUESTION_TYPE_PROMPTS ||--o{ ANSWER_GENERATION_BATCHES : single_choice_prompt
    ANSWER_QUESTION_TYPE_PROMPTS ||--o{ ANSWER_GENERATION_BATCHES : multiple_choice_prompt
    ANSWER_QUESTION_TYPE_PROMPTS ||--o{ ANSWER_GENERATION_BATCHES : simple_fact_prompt
    ANSWER_QUESTION_TYPE_PROMPTS ||--o{ ANSWER_GENERATION_BATCHES : subjective_prompt

    ANSWER_GENERATION_BATCHES ||--o{ MODEL_ANSWER_RUNS : batch_contains_runs
    MODEL_ANSWER_RUNS ||--o{ LLM_ANSWERS : run_generates_answers
    MODEL_ANSWER_RUNS ||--o{ EVALUATION_RUNS : run_evaluated_by
    MODEL_ANSWER_RUNS ||--o{ ANSWER_GENERATION_BATCHES : last_processed_run

    DATASET_QUESTION_MAPPING ||--o{ LLM_ANSWERS : mapping_answered_by_llm

    LLM_ANSWERS ||--o{ EVALUATIONS : answer_evaluated

    EVALUATORS ||--o{ EVALUATION_RUNS : evaluator_runs
    EVALUATORS ||--o{ EVALUATIONS : evaluator_evaluates

    EVALUATION_PROMPT_ASSEMBLY_CONFIGS ||--o{ EVALUATION_RUNS : config_used_in_eval_runs
    EVALUATION_SUBJECTIVE_PROMPTS ||--o{ EVALUATION_RUNS : prompt_used_in_eval_runs

    EVALUATION_RUNS ||--o{ EVALUATIONS : run_contains_evaluations

    EVALUATIONS ||--o{ EVALUATION_DETAILS : evaluation_has_details

    EVALUATION_CRITERIA ||--o{ EVALUATION_DETAILS : criteria_used_in_details
    EVALUATION_CRITERIA ||--o{ EVALUATION_CRITERIA : criteria_versioning

    ANSWER_GENERATION_BATCHES ||--o{ MODEL_BATCH_SCORES : batch_scored
    LLM_MODELS ||--o{ MODEL_BATCH_SCORES : model_scored
    EVALUATORS ||--o{ MODEL_BATCH_SCORES : evaluator_scores
```