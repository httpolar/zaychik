pub mod info;

pub async fn get_commands() -> Vec<poise::Command<crate::Data, crate::Error>> {
    vec![
        self::info::avatar(),
        self::info::userinfo(),
        self::info::ping(),
    ]
}
