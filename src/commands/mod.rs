mod external_apis;
mod info;
mod random;

pub async fn get_commands() -> Vec<poise::Command<crate::Data, anyhow::Error>> {
    vec![
        self::info::avatar(),
        self::info::userinfo(),
        self::info::ping(),
        self::random::random(),
        self::external_apis::sauce(),
        self::settings::settings(),
    ]
}
