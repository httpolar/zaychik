mod external_apis;
mod info;
mod random;
mod react_roles;
mod settings;

pub async fn get_commands() -> Vec<poise::Command<crate::Data, anyhow::Error>> {
    vec![
        self::info::avatar(),
        self::info::userinfo(),
        self::info::ping(),
        self::random::random(),
        self::external_apis::sauce(),
        self::react_roles::reactrole(),
        self::settings::settings(),
    ]
}
